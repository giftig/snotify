package com.xantoria.snotify.backoff

import scala.concurrent.duration._

/**
 * An exponential backoff strategy where each new attempt happens after 2^n seconds
 */
class ExponentialBackoffStrategy(
  override protected val maxRetries: Int = 10
) extends LimitedBackoffStrategy {
  override protected def whileWithinLimit(attempt: Int): FiniteDuration = {
    math.pow(2, attempt).seconds
  }
}
