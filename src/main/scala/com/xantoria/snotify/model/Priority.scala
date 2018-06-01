package com.xantoria.snotify.model

object Priority {
  val Max = 100
  val Critical = 90
  val High = 75
  val Medium = 50
  val Low = 25
  val Trivial = 10
  val Min = 0

  val Default = Medium

  def isValid(i: Int): Boolean = i >= Min && i <= Max
  def isValid(bd: BigDecimal): Boolean = bd.isValidInt && isValid(bd.toInt)
}
