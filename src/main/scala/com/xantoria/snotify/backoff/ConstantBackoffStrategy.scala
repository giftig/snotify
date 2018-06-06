package com.xantoria.snotify.backoff

import scala.concurrent.duration._

class ConstantBackoffStrategy(
  d: FiniteDuration = 30.seconds,
  override protected val maxRetries: Int = 10
) extends LimitedBackoffStrategy {
  def whileWithinLimit(attempt: Int): FiniteDuration = d
}
