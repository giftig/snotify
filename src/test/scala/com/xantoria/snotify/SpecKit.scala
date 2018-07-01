package com.xantoria.snotify

import org.scalatest._
import org.scalatest.concurrent._

/**
 * Superclass of all test classes; defines the standard testing tools
 */
trait SpecKit extends FlatSpecLike with Matchers with ScalaFutures
