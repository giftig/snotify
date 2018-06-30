package com.xantoria.snotify.utils

import com.xantoria.snotify.SpecKit

class StringsSpec extends SpecKit {
  "Strings.truncate" should "leave a short string intact" in {
    val limit = 30
    val short = "s" * (limit - 1)
    val exact = "s" * limit

    Strings.truncate(short, limit) should be(short)
    Strings.truncate(exact, limit) should be(exact)
  }

  it should "truncate a longer string with an ellipsis" in {
    val limit = 30
    val longer = "s" * (limit * 2)
    val expected = "s" * (limit - 1) + "…"

    Strings.truncate(longer, limit) should be(expected)
  }

  it should "use some other character if desired" in {
    val limit = 30
    val c = 'c'
    val longer = "s" * (limit * 2)
    val expected = "s" * (limit - 1) + c

    Strings.truncate(longer, limit, c) should be(expected)
  }

  it should "work for different string lengths" in {
    val limits = Seq(10, 20, 30, 100)
    limits foreach { l =>
      val longer = "s" * (l * 2)
      Strings.truncate(longer, l) should have length(l)
    }
  }
}
