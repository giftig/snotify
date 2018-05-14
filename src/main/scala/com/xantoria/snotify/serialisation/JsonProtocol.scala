package com.xantoria.snotify.serialisation

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import spray.json._

import com.xantoria.snotify.model.Notification

object JsonProtocol extends DefaultJsonProtocol {
  private val datePattern: String = "yyyy-MM-dd HH:mm:ss"
  private val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern(datePattern)

  /**
   * Shortcut to require that a specific field be a String, optionally with a max length
   *
   * Handles triggering any appropriate error messages if the field does not conform
   */
  private def requireString(
    v: JsValue,
    errorDesc: String,
    maxLength: Option[Int] = None
  ): String = {
    v match {
      case JsString(s) => {
        maxLength foreach {
          len => if (s.length > len) serializationError(s"Value too long: $errorDesc")
        }
        s
      }
      case _ => serializationError(s"Wrong type: $errorDesc")
    }
  }

  /**
   * Convenience overload; allows handling the case where the value is absent as well
   */
  private def requireString(
    v: Option[JsValue],
    errorDesc: String,
    maxLength: Option[Int]
  ): String = v map {
    requireString(_, errorDesc, maxLength)
  } getOrElse deserializationError(s"Missing value: $errorDesc")

  implicit object NotificationFormat extends RootJsonFormat[Notification] {
    def write(n: Notification): JsValue = JsObject(Map(
      "id" -> JsString(n.id),
      "body" -> JsString(n.body),
      "title" -> (n.title map { JsString(_) } getOrElse JsNull),
      "targets" -> JsArray(n.targets.map { JsString(_) }.toVector),
      "triggerTime" -> JsString(n.triggerTime.toString(datePattern)),
      "creationTime" -> {
        n.creationTime map { t => JsString(t.toString(datePattern)) } getOrElse JsNull
      },
      "source" -> (n.source map { JsString(_) } getOrElse JsNull),
      "complete" -> JsBoolean(n.complete)
    ))

    def read(data: JsValue): Notification = {
      val fields = data.asJsObject.fields
      import Notification.{id => _, _}

      Notification(
        id = fields.get("id") map {
          case JsString(s) => s
          case _ => deserializationError("Wrong type for notification id")
        } getOrElse Notification.id,
        body = requireString(fields.get("body"), "notification body", Some(MaxBodyLen)),
        title = fields.get("title") map {
          v: JsValue => requireString(v, "notification title", Some(MaxTitleLen))
        },
        targets = fields.get("targets") map {
          case JsArray(targets) if targets.nonEmpty => targets map {
            v: JsValue => requireString(v, "notification target", Some(MaxTargetLen))
          }
          case JsArray(_) => deserializationError("Notification targets cannot be empty")
          case _ => deserializationError("Wrong type for notification targets")
        } getOrElse deserializationError("Missing notification targets"),
        triggerTime = DateTime.parse(
          requireString(fields.get("trigger_time"), "trigger time", maxLength = None),
          dateFormatter
        ),
        creationTime = fields.get("creation_time") map {
          v: JsValue => {
            val t = requireString(v, "notification creation time", maxLength = None)
            DateTime.parse(t, dateFormatter)
          }
        },
        source = fields.get("source") map {
          v: JsValue => requireString(v, "notification source", Some(MaxTargetLen))
        },
        complete = fields.get("boolean") map {
          case JsBoolean(b: Boolean) => b
          case _ => deserializationError("Wrong type for notification 'complete' field")
        } getOrElse false
      )
    }
  }
}
