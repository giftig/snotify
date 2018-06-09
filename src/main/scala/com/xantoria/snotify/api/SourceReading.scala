package com.xantoria.snotify.api

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.{Notification, ReceivedNotification}
import com.xantoria.snotify.persist.StreamingPersistence

trait NotificationReading extends StrictLogging {
  protected val persistStreamer: StreamingPersistence
  protected val scheduler: ActorRef

  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  import actorSystem.dispatcher

  private lazy val notificationSources: Seq[NotificationSource[ReceivedNotification]] = {
    Config.notificationReaders map { c =>
      val reader = c.newInstance
      reader match {
        case r: NotificationSource[ReceivedNotification] => r
        case _ => throw new IllegalArgumentException(s"Bad notification reader class ${c.getName}")
      }
    }
  }

  /**
   * Run the stream which reads notifications from sources, persists them, and schedules them
   *
   * This may come from various sources and combined to form the source of the stream, depending on
   * configuration
   */
  def runSources(): Unit = {
    logger.info("Running notification source streams...")

    // TODO: This should stay in NotificationReading, in another method
    val src: Source[ReceivedNotification, NotUsed] = {
      val merged = notificationSources map {
        _.source()
      } reduce { _.merge(_) }

      RestartSource.withBackoff(
        minBackoff = 3.seconds,
        maxBackoff = 15.seconds,
        randomFactor = 0.2,
        maxRestarts = 15
      ) { () => merged }
    }

    // TODO: Next thing is to move this (and this method) into a controller class which brings
    // together all the disparate components
    val g = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merger = builder.add(Merge[Notification](2))
      val sink = Sink.actorRef(scheduler, Done)

      src ~> persistStreamer.persistFlow ~> merger
      persistStreamer.persistedSource ~> merger

      merger ~> sink

      ClosedShape
    }

    RunnableGraph.fromGraph(
      g.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    ).run()
  }
}
