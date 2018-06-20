package com.xantoria.snotify.dao

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification

/**
 * Storage implementation which acknowledges notifications but makes no attempt to store them
 *
 * i.e. notifications will only exist in memory as scheduled akka messages, and will be
 * acknowledged in the queue as soon as they're read.
 *
 * **WARNING**: This is likely only to be useful for testing purposes, unless you only use
 *              short-term notifications and don't care if you miss some.
 */
class NoStorage extends Persistence with StrictLogging {
  import Persistence._

  /**
   * Noop; always return successfully and indicate that the notification was new
   */
  override def save(n: Notification)(implicit ec: ExecutionContext): Future[WriteResult] = {
    logger.warn(s"Did not persist notification ${n.id}")
    Future.successful(Inserted)
  }

  /**
   * Always return Future(Nil)
   */
  override def findPending()(implicit ec: ExecutionContext): Future[Seq[Notification]] = {
    Future.successful(Nil)
  }

  /**
   * Noop; always return successfully
   */
  override def markComplete(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = {
    Future.unit
  }

  /**
   * Noop; always return successfully and indicate that the notification was new
   */
  override def markFailed(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = {
    Future.unit
  }
}
