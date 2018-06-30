package com.xantoria.snotify.utils

import com.xantoria.snotify.SpecKit

class PriorityTranslatorSpec extends SpecKit {
  private val translator = new PriorityTranslator[String](Map(
    10 -> "low",
    25 -> "medium",
    50 -> "high"
  ))

  "A priority translator" should "handle thresholds properly" in {
    val tests = Map(
      5 -> "low",
      10 -> "medium",
      22 -> "medium",
      24 -> "medium",
      25 -> "high",
      49 -> "high",
      99 -> "high"
    )

    tests foreach { case (k, v) => translator(k) should be(v) }
  }
}
