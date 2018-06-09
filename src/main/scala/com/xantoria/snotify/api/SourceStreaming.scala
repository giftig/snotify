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
import com.xantoria.snotify.persist.Persistence

trait SourceStreaming extends StrictLogging {
  protected val scheduler: ActorRef
  protected val persistHandler: Persistence
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

  // TODO: These flows can be separated out

  /**
   * Construct a flow which persists a given notification locally
   */
  val persistenceSteps: Flow[ReceivedNotification, Notification, NotUsed] = {
    Flow[ReceivedNotification].mapAsync(Config.persistThreads) {
      n: ReceivedNotification => try {
        val saved: Future[Unit] = persistHandler.save(n.notification)
        saved foreach { _ => n.ack() }
        saved map { _ => n.notification }
      } catch {
        case NonFatal(t) => {
          logger.error(s"Unexpected error persisting notification $n", t)
          n.retry()
          throw t
        }
      }
    }
  }

  val handleAlerts: Sink[Notification, NotUsed] = Sink.actorRef(scheduler, Done)

  val persistAndSchedule: Sink[ReceivedNotification, NotUsed] = persistenceSteps.to(handleAlerts)

  /**
   * Run the stream which reads notifications from sources, persists them, and schedules them
   *
   * This may come from various sources and combined to form the source of the stream, depending on
   * configuration
   */
  def runSources(): Unit = {
    logger.info("Running notification source streams...")

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

    val persistedSrc: Source[Notification, NotUsed] = Source.fromFuture(
      persistHandler.findPending()
    ) mapConcat { _.toList }

    val g = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merger = builder.add(Merge[Notification](2))

      src ~> persistenceSteps ~> merger
      persistedSrc ~> merger

      merger ~> handleAlerts

      ClosedShape
    }

    RunnableGraph.fromGraph(
      g.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    ).run()
  }
}
