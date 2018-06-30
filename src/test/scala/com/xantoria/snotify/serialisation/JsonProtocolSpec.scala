package com.xantoria.snotify.serialisation

import spray.json._

import com.xantoria.snotify.SpecKit
import com.xantoria.snotify.model._
import com.xantoria.snotify.utils.TestUtils

import JsonProtocol._

class JsonProtocolSpec extends SpecKit {
  "The notification spray-json protocol" should "symmetrically handle notifications" in {
    // Read some fixtures from our test fixtures dir and convert them to spray json AST
    val fixtures = Seq("future", "hodor1", "hodor2", "hodor3") map {
      s => TestUtils.loadFixture(s"notifications/$s.json").parseJson
    }

    // Convert to and from the Notification model twice, ensuring the Notification is unchanged
    fixtures foreach { f =>
      val once = f.convertTo[Notification]
      val twice = once.toJson.convertTo[Notification]
      once should be(twice)
    }
  }
}
