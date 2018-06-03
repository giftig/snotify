package com.xantoria.snotify.alert.pushover

import spray.json._

/**
 * Spray protocol for the Pushover API
 */
object JsonProtocol extends DefaultJsonProtocol {
  implicit val messageFormat = jsonFormat12(PushoverMessage.apply)
}
