package com.xantoria.snotify.persist

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
  /**
   * Noop; always return successfully
   */
  override def save(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.warn(s"Did not persist notification ${n.id}")
    Future.unit
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
}
