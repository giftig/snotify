package com.xantoria.snotify.dao

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

/**
 * Defines the basic operations required to persist and fetch notifications for scheduling
 */
trait Persistence extends StrictLogging {
  /**
   * Persist a notification to be delivered later
   */
  def save(n: Notification)(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Find notifications which are not yet complete
   */
  def findPending()(implicit ec: ExecutionContext): Future[Seq[Notification]]

  /**
   * Mark the specified notification as complete
   */
  def markComplete(n: Notification)(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Mark the specified notification as undeliverable
   */
  def markFailed(n: Notification)(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Accepts `ReceivedNotifications` and persists them, feeding back via tha notification's API
   *
   * Strips the notifications down to the raw `Notification` for convenience as the wrapper is no
   * longer needed once it's been persisted
   */
  def save(rn: ReceivedNotification)(implicit ec: ExecutionContext): Future[Notification] = try {
    // TODO: Refactor this into the underlying Persistence as a util method?
    val saved: Future[Unit] = save(rn.notification)
    saved foreach { _ =>
      logger.info(s"Successfully wrote $rn")
      rn.ack()
    }
    saved map { _ => rn.notification }
  } catch {
    case NonFatal(t) => {
      logger.error(s"Unexpected error persisting $rn", t)
      rn.retry()
      throw t
    }
  }
}

object Persistence {
  class NotificationConflict(id: String) extends IllegalStateException(
    s"The notification $id has already been saved"
  )
}
