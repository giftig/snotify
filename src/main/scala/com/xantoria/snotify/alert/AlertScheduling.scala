package com.xantoria.snotify.alert

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime

import com.xantoria.snotify.backoff.BackoffStrategy
import com.xantoria.snotify.dao.Persistence
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.utils.{Time => TimeUtils}

trait AlertScheduling extends StrictLogging {
  implicit val context: ActorContext
  protected val alertTarget: ActorRef

  import AlertScheduling._
  import context.dispatcher

  protected val alertHandler: AlertHandling
  protected val notificationDao: Persistence
  protected val backoffStrategy: BackoffStrategy

  protected def triggerAlert(n: Notification, attempt: Int = 0): Future[Unit] = {
    logger.info {
      val attemptClause = if (attempt == 0) "" else s" (attempt #${attempt + 1})"
      s"Triggering alerts for notification ${n.id}$attemptClause"
    }
    val result: Future[Boolean] = alertHandler.triggerAlert(n) recover {
      case t: Throwable => {
        logger.error(s"Unexpected exception triggering notification ${n.id}", t)
        false
      }
    }

    result map {
      case true => notificationDao.markComplete(n)
      case _ => {
        logger.warn(s"Notification ${n.id} was marked unacknowledged or delivery failed")
        if (!rescheduleNotification(n, attempt + 1)) {
          notificationDao.markFailed(n)
        }
      }
    }
  }

  protected def scheduleNotification(n: Notification): Unit = {
    if (isSoonEnough(n)) {
      context.system.scheduler.scheduleOnce(
        TimeUtils.timeUntil(n.triggerTime),
        alertTarget,
        TriggerAlert(n)
      )
    } else {
      logger.info(s"Not scheduling delivery of notification ${n.id} yet; saving for later")
    }
  }

  /**
   * Reschedule the notification delivery according to our backoff strategy
   *
   * @returns true if we've scheduled another attempt; false if we're giving up
   */
  protected def rescheduleNotification(n: Notification, attempt: Int = 1): Boolean = {
    backoffStrategy.delay(attempt) map { d =>
      logger.info(s"Triggering redelivery of notification ${n.id} in $d")
      context.system.scheduler.scheduleOnce(d, alertTarget, TriggerAlert(n, attempt))
      true
    } getOrElse {
      logger.warn(s"Not rescheduling delivery of notification ${n.id} after attempt $attempt")
      false
    }
  }
}

object AlertScheduling {
  case class TriggerAlert(n: Notification, attempt: Int = 0)

  /**
   * Determine whether the given notification will be triggered "soon"
   *
   * Soon is an arbitrary period but essentially defines how long we're willing to have a
   * notification in memory awaiting an alert rather than leaving it in the db and picking it up
   * later.
   */
  def isSoonEnough(n: Notification): Boolean = {
    val threshold = 3.days.toMillis  // TODO: Configurable, based on a db re-read interval?
    val diff = n.triggerTime.getMillis - DateTime.now.getMillis
    diff < threshold
  }
}
