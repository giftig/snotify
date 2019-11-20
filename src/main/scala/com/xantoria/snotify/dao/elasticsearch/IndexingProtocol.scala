package com.xantoria.snotify.dao.elasticsearch

import scala.util.Try

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import spray.json._

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._

trait IndexingProtocol {
  implicit object IndexableNotification extends Indexable[Notification] {
    override def json(n: Notification): String = n.toJson.compactPrint
  }

  implicit object NotificationReader extends HitReader[Notification] {
    override def read(h: Hit): Try[Notification] = Try {
      h.sourceAsString.parseJson.convertTo[Notification]
    }
  }
}

object IndexingProtocol extends IndexingProtocol
