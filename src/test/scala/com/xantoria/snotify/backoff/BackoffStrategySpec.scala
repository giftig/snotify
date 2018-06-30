package com.xantoria.snotify.backoff

import scala.concurrent.duration._

import com.xantoria.snotify.SpecKit

class BackoffStrategySpec extends SpecKit {
  "Limited backoff strategy" should "delegate to whileWithinLimit" in {
    val lim = 3
    val backoff = new BackoffStrategySpec.TestLimitedBackoffStrategy(lim)
    backoff.delay(0) should be(Some(Duration.Zero))
    backoff.delay(lim) should be(Some(Duration.Zero))
  }

  it should "be undefined if attempts > maxRetries" in {
    val lim = 3
    val backoff = new BackoffStrategySpec.TestLimitedBackoffStrategy(lim)
    backoff.delay(lim + 1) should be(None)
  }

  "Constant backoff strategy" should "always return the same value" in {
    val d = 30.seconds
    val backoff = new ConstantBackoffStrategy(d, maxRetries = 1000)
    val retries = Seq(1, 2, 3, 10, 11, 12, 100, 101, 102)

    retries foreach {
      r => backoff.delay(r) should be(Some(d))
    }
  }

  "Exponential backoff strategy" should "delay exponentially" in {
    val backoff = new ExponentialBackoffStrategy(maxRetries = 1000)
    val series = Seq(1, 2, 4, 8, 16, 32, 64, 128, 256) map { _.seconds }

    series.zipWithIndex foreach {
      case (expected, r) => backoff.delay(r) should be(Some(expected))
    }
  }

  "Never retry strategy" should "always return None" in {
    NeverRetryStrategy.delay(0) should be(None)
    NeverRetryStrategy.delay(1) should be(None)
    NeverRetryStrategy.delay(10) should be(None)
    NeverRetryStrategy.delay(1337) should be(None)
  }
}

object BackoffStrategySpec {
  class TestLimitedBackoffStrategy(override val maxRetries: Int) extends LimitedBackoffStrategy {
    override protected def whileWithinLimit(attempt: Int): FiniteDuration = Duration.Zero
  }
}
