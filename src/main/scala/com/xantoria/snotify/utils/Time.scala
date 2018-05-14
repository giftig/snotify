package com.xantoria.snotify.utils

import scala.concurrent.duration._

import org.joda.time.DateTime

object Time {
  /**
   * Get the length of time until the specified time, relative to now
   *
   * Zero duration if the specified time is in the past.
   */
  def timeUntil(d: DateTime): FiniteDuration = {
    val millis = d.getMillis - DateTime.now.getMillis
    val dur = millis.millis
    if (dur <= Duration.Zero) Duration.Zero else dur
  }
}
