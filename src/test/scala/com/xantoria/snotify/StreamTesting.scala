package com.xantoria.snotify

import akka.testkit.TestKit
import akka.stream.{ActorMaterializer, Materializer}

/**
 * A test trait which provides a materializer based on akka testkit
 */
trait StreamTesting {
  _: TestKit =>

  implicit val mat: Materializer = ActorMaterializer()
}
