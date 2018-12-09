package com.xantoria.snotify.dao

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

/**
 * Defines the basic operations required to persist and fetch notifications for scheduling
 */
trait Persistence extends StrictLogging {
  import Persistence._

  /**
   * Perform steps needed to boot strap the db for the first time if appropriate
   *
   * This may include creating tables or indexes, or anything else required to bootstrap the
   * backend from a fresh instance. It *must* be idempotent and must handle any initialisation
   * issues as gracefully as possible. It will be called on service startup.
   *
   * The default implementation is a noop, so subclasses don't have to implement if if they don't
   * need it.
   */
  def init()(implicit ec: ExecutionContext): Future[Unit] = Future.unit

  /**
   * Persist a notification to be delivered later
   */
  def save(n: Notification)(implicit ec: ExecutionContext): Future[WriteResult]

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
   *
   * @returns The notification if it's been written for the first time; None otherwise (indicating
   *          that the notification does not need to be handled again)
   */
  def save(rn: ReceivedNotification)(
    implicit ec: ExecutionContext
  ): Future[Option[Notification]] = {
    val saved: Future[WriteResult] = save(rn.notification)

    // Due to the ReceivedNotification API, acking / rejecting notifications is a side-effect sadly
    saved onComplete {
      case Success(Inserted) | Success(Updated) => {
        logger.info(s"Successfully wrote $rn")
        rn.stored()
      }
      case Success(Ignored) => {
        logger.info(s"Acknowledged $rn but did not overwrite existing notification")
        rn.ignored()
      }
      case Failure(NonFatal(t)) => {
        logger.error(s"Unexpected error persisting $rn", t)
        rn.error(t)
      }
    }

    saved map {
      case Inserted => Some(rn.notification)
      case _ => None
    } recover {
      case NonFatal(_) => None
    }
  }
}

object Persistence {
  sealed trait WriteResult
  case object Inserted extends WriteResult
  case object Updated extends WriteResult
  case object Ignored extends WriteResult
}
