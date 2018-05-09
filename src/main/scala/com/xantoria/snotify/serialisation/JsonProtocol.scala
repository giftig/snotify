package com.xantoria.snotify.serialisation

import spray.json._

import com.xantoria.snotify.model.Notification

object JsonProtocol extends DefaultJsonProtocol {
  implicit object NotificationFormat extends RootJsonFormat[Notification] {
    def write(n: Notification): JsValue = JsObject(Map(
      "id" -> JsString(n.id),
      "body" -> JsString(n.body),
      "title" -> (n.title map { JsString(_) } getOrElse JsNull),
      "targets" -> JsArray(n.targets.map { JsString(_) }.toList)
    ))

    def read(data: JsValue): Notification = {
      val fields = data.asJsObject.fields
      Notification(
        id = fields.get("id") map {
          case JsString(s) => s
          case _ => deserializationError("Wrong type for notification id")
        } getOrElse Notification.id,
        body = fields.get("body") map {
          case JsString(body) => body
          case _ => deserializationError("Wrong type for notification body")
        } getOrElse { deserializationError("Missing notification body") },
        title = fields.get("title") map {
          case JsString(title) => title
          case _ => deserializationError("Wrong type for notification title")
        },
        targets = fields.get("targets") map {
          case targets: JsArray if targets.elements.nonEmpty => {
            targets.elements map {
              case JsString(s) => s
              case _ => deserializationError("Notification targets must be strings")
            }
          }
          case JsArray(_) => deserializationError("Notification targets cannot be empty")
          case _ => deserializationError("Wrong type for notification targets")
        } getOrElse deserializationError("Missing notification targets")
      )
    }
  }
}
