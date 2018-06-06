package com.xantoria.snotify.config

import scala.concurrent.duration._

import com.typesafe.config.{Config => TConfig}

import com.xantoria.snotify.backoff._

object ConfigHelpers {
  implicit class PimpedConfig(c: TConfig) {
    def getBackoffStrategy(key: String): BackoffStrategy = {
      val cfg = c.getConfig(key)
      val t = cfg.getString("type")
      lazy val retries = cfg.getInt("max-retries")
      lazy val delay = Duration.fromNanos(cfg.getDuration("delay").toNanos)

      t match {
        case "constant" => new ConstantBackoffStrategy(d = delay, maxRetries = retries)
        case "exponential" => new ExponentialBackoffStrategy(maxRetries = retries)
        case "never-retry" => NeverRetryStrategy
        case _ => throw new IllegalArgumentException("Invalid backoff strategy type")
      }
    }
  }
}
