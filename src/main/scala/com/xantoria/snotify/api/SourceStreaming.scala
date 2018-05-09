package com.xantoria.snotify.api

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification

trait SourceStreaming extends StrictLogging {
  protected val alertHandler: ActorRef
  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  private def notificationSources: Seq[NotificationSource[ReceivedNotification]] = {
    // FIXME: Load from config by name
    Seq(new com.xantoria.snotify.queue.QueueHandler)
  }

  /**
   * Construct a flow which persists a given notification locally
   */
  private def persistenceSteps: Flow[ReceivedNotification, ReceivedNotification, NotUsed] = ???

  private val handleAlerts: Sink[ReceivedNotification, NotUsed] = Sink.actorRef(alertHandler, Done)

  /**
   * Run the stream which reads notifications from sources, persists them, and schedules them
   *
   * This may come from various sources and combined to form the source of the stream, depending on
   * configuration
   */
  def runSources(): Unit = {
    logger.info("Running notification source streams...")

    val src: Source[ReceivedNotification, NotUsed] = notificationSources map {
      _.source()
    } reduce { _.merge(_) }

    src
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
      .runWith(handleAlerts)
  }
}
