package com.xantoria.snotify.utils

import com.typesafe.config.ConfigFactory

import com.xantoria.snotify.SpecKit

class PriorityTranslatorSpec extends SpecKit {
  private val translator = new PriorityTranslator[String](Map(
    10 -> "low",
    25 -> "medium",
    50 -> "high"
  ))

  private val tests = Map(
    5 -> "low",
    10 -> "medium",
    22 -> "medium",
    24 -> "medium",
    25 -> "high",
    49 -> "high",
    99 -> "high"
  )

  "A priority translator" should "handle thresholds properly" in {
    tests foreach { case (k, v) => translator(k) should be(v) }
  }

  it should "be readable from config" in {
    val cfg = ConfigFactory.parseString(
      """
      10 = low
      25 = medium
      50 = high
      """
    )
    val res = PriorityTranslator.fromConfig(cfg.root) { _.unwrapped.asInstanceOf[String] }
    tests foreach { case (k, v) => res(k) should be(v) }
  }
}
