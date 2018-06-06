package com.xantoria.snotify.backoff

import scala.concurrent.duration.FiniteDuration

/**
 * Represents a strategy for backoff while retrying delivery of a notification
 */
trait BackoffStrategy {
  /**
   * The next delay to use; None if we should give up at this point
   */
  def delay(attempt: Int): Option[FiniteDuration]
}

/**
 * A backoff strategy valid up until a max number of attempts
 */
trait LimitedBackoffStrategy extends BackoffStrategy {
  protected val maxRetries: Int

  protected def whileWithinLimit(attempt: Int): FiniteDuration

  override def delay(attempt: Int): Option[FiniteDuration] = if (attempt <= maxRetries) {
    Some(whileWithinLimit(attempt))
  } else None
}
