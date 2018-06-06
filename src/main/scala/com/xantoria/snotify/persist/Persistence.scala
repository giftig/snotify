package com.xantoria.snotify.persist

import scala.concurrent.{ExecutionContext, Future}

import com.xantoria.snotify.model.Notification

/**
 * Defines the basic operations required to persist and fetch notifications for scheduling
 */
trait Persistence {
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
}

object Persistence {
  class NotificationConflict(id: String) extends IllegalStateException(
    s"The notification $id has already been saved"
  )
}
