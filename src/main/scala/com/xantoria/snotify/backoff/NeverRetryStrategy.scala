package com.xantoria.snotify.backoff

import scala.concurrent.duration.FiniteDuration

/**
 * Never retry on failure
 */
object NeverRetryStrategy extends BackoffStrategy {
  override def delay(attempt: Int): Option[FiniteDuration] = None
}
