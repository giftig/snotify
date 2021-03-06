package com.xantoria.snotify.streaming

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._

import com.xantoria.snotify.dao.StreamingPersistence
import com.xantoria.snotify.model.{Notification, ReceivedNotification}

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
  protected val directSource: NotificationSource[ReceivedNotification]
  protected val clusterSource: ClusterHandling[ReceivedNotification]

  protected val streamingDao: StreamingPersistence
  protected val scheduler: ActorRef

  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  /**
   * Start the primary graph which connects sources through to the internal persist/schedule system
   *
   * Expose an ActorRef for an input hook as an additional Source alongside the NotificationSource
   * system. This allows sending additional notifications from other sources via the actor system
   * by materialising the graph as an ActorRef
   */
  protected val graph: RunnableGraph[ActorRef] = {
    val c = clusterSource.source
    val g = GraphDSL.create(c) { implicit b: GraphDSL.Builder[ActorRef] => cluster =>
      import GraphDSL.Implicits._

      val mergeClusterAndDirect = b.add(Merge[ReceivedNotification](2))
      val mergeWithDao = b.add(Merge[Notification](2))
      val sink = Sink.actorRef(scheduler, Done)

      cluster ~> mergeClusterAndDirect
      directSource.source() ~> mergeClusterAndDirect

      mergeClusterAndDirect ~> streamingDao.persistFlow ~> mergeWithDao
      streamingDao.periodicSource ~> mergeWithDao

      mergeWithDao ~> sink

      ClosedShape
    }

    RunnableGraph.fromGraph(
      g.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    )
  }

}

