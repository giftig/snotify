package com.xantoria.snotify.utils

import scala.concurrent.duration._

import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.matchers._

import com.xantoria.snotify.SpecKit

class TimeSpec extends SpecKit {
  import TimeSpec._

  "timeUntil" should "produce the correct durations" in {
    val now = DateTime.now
    val backwardTests: Seq[DateTime] = Seq(
      now,
      now.minusSeconds(10),
      now.minusHours(8),
      now.minusYears(30)
    )
    val forwardTests: Map[DateTime, FiniteDuration] = Map(
      now.plusMinutes(5) -> 5.minutes,
      now.plusMinutes(30) -> 30.minutes,
      now.plusHours(8) -> 8.hours,
      now.plusDays(7) -> 7.days
    )

    backwardTests foreach {
      t => Time.timeUntil(t) should be(Duration.Zero)
    }
    forwardTests foreach {
      case (t, expected) => Time.timeUntil(t) should beApprox(expected)
    }
  }
}

object TimeSpec {
  class ApproximateTimeMatcher(expected: FiniteDuration) extends Matcher[FiniteDuration] {
    def apply(actual: FiniteDuration): MatchResult = {
      val diff = (actual - expected).toSeconds
      val res = math.abs(diff) < 10

      MatchResult(
        res,
        s"Duration $actual was significantly different to $expected",
        s"Duration $actual was approximately equal to $expected"
      )
    }
  }

  def beApprox(exp: FiniteDuration): ApproximateTimeMatcher = new ApproximateTimeMatcher(exp)
}
