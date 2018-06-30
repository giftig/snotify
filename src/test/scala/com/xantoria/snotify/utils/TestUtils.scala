package com.xantoria.snotify.utils

import scala.io.Source

object TestUtils {
  /**
   * Load a fixture from the test resources
   *
   * @param path Resource path relative to /fixtures
   */
  def loadFixture(path: String): String = Source.fromResource(s"fixtures/$path").mkString
}
