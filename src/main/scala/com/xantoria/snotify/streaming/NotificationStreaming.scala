package com.xantoria.snotify.streaming

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._

import com.xantoria.snotify.model.{Notification, ReceivedNotification}
import com.xantoria.snotify.persist.StreamingPersistence

/**
 * Represents the notification streaming portion of the application
 *
 * This merely provides a graph pieced together from other stream components provided by the
 * relevant traits.
 *
 * Accepts notifications in from a number of sources via streams and runs them through the
 * persistence and scheduling steps common to other notification sources
 *
 * The provided graph also includes reading from the database to schedule any missed notifications
 *
 * It does **not** include some aspects of notification handling prior to scheduling if they are
 * provided from sources which are not registered as a constant stream; e.g. the REST API reuses
 * the same streams to register single notifications when it receives requests.
 */
trait NotificationStreaming {
  protected val source: NotificationSource[ReceivedNotification]
  protected val streamingDao: StreamingPersistence
  protected val scheduler: ActorRef

  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  protected val graph: RunnableGraph[NotUsed] = {
    val g = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merger = builder.add(Merge[Notification](2))
      val sink = Sink.actorRef(scheduler, Done)

      source.source() ~> streamingDao.persistFlow ~> merger
      streamingDao.persistedSource ~> merger

      merger ~> sink

      ClosedShape
    }

    RunnableGraph.fromGraph(
      g.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    )
  }

}

