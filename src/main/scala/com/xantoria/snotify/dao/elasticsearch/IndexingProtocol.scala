package com.xantoria.snotify.dao.elasticsearch

import com.sksamuel.elastic4s.Indexable
import spray.json._

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._

trait IndexingProtocol {
  implicit object IndexableNotification extends Indexable[Notification] {
    override def json(n: Notification): String = n.toJson.compactPrint
  }
}

object IndexingProtocol extends IndexingProtocol
