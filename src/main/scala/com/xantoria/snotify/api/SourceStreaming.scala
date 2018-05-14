package com.xantoria.snotify.api

import scala.concurrent.Future
import scala.util.control.NonFatal

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.persist.Persistence

trait SourceStreaming extends StrictLogging {
  protected val alertHandler: ActorRef
  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  import actorSystem.dispatcher

  private def notificationSources: Seq[NotificationSource[ReceivedNotification]] = {
    Config.notificationReaders map { c =>
      val reader = c.newInstance
      reader match {
        case r: NotificationSource[ReceivedNotification] => r
        case _ => throw new IllegalArgumentException(s"Bad notification reader class ${c.getName}")
      }
    }
  }

  /**
   * Construct a flow which persists a given notification locally
   */
  private def persistenceSteps: Flow[ReceivedNotification, ReceivedNotification, NotUsed] = {
    val handler: Persistence = Config.persistHandler.newInstance match {
      case p: Persistence => p
      case _ => throw new IllegalArgumentException(
        s"Bad persistence class ${Config.persistHandler.getName}"
      )
    }

    Flow[ReceivedNotification].mapAsync(Config.persistThreads) {
      n: ReceivedNotification => try {
        val saved: Future[Unit] = handler.save(n.notification)
        saved foreach { _ => n.ack() }
        saved map { _ => n }
      } catch {
        case NonFatal(t) => {
          logger.error(s"Unexpected error persisting notification $n", t)
          n.retry()
          throw t
        }
      }
    }
  }

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
      .via(persistenceSteps)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
      .runWith(handleAlerts)
  }
}
