package com.xantoria.snotify.alert

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.persist.Persistence
import com.xantoria.snotify.utils.{Time => TimeUtils}

trait AlertScheduling extends StrictLogging {
  _: Actor =>

  import AlertScheduling._
  import context.dispatcher

  protected val alertHandler: AlertHandling
  protected val persistHandler: Persistence

  // TODO: Use a scaling backoff mechanism
  private val rescheduleDelay: FiniteDuration = 1.minutes

  protected def triggerAlert(n: Notification): Future[Unit] = {
    val result: Future[Boolean] = alertHandler.triggerAlert(n)
    result map {
      case true => persistHandler.markComplete(n)
      case _ => {
        logger.warn(s"Notification ${n.id} was marked unacknowledged, rescheduling")
        rescheduleNotification(n)
      }
    }
  }

  protected def scheduleNotification(n: Notification): Unit = {
    context.system.scheduler.scheduleOnce(
      TimeUtils.timeUntil(n.triggerTime),
      self,
      TriggerAlert(n)
    )
  }

  protected def rescheduleNotification(n: Notification): Unit = {
    context.system.scheduler.scheduleOnce(rescheduleDelay, self, TriggerAlert(n))
  }
}

object AlertScheduling {
  case class TriggerAlert(n: Notification)
}

class AlertScheduler(
  override protected val alertHandler: AlertHandling,
  override protected val persistHandler: Persistence
) extends Actor with AlertScheduling {
  import AlertScheduling._

  def receive: Receive = {
    // TODO: Consider akka-quartz-scheduler
    case n: Notification => scheduleNotification(n)
    case TriggerAlert(n: Notification) => triggerAlert(n)
  }
}
