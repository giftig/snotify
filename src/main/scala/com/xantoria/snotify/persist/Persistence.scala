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
   * Mark the specified notification ID as complete
   */
  def markComplete(id: String)(implicit ec: ExecutionContext): Future[Unit]
}
