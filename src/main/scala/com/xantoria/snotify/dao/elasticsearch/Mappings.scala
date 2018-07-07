package com.xantoria.snotify.dao.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl._

object Mappings {
  val Notification = mapping("notification").as(
    keywordField("id"),
    textField("body"),
    textField("title"),
    keywordField("targets"),
    dateField("trigger_time"),
    dateField("creation_time"),
    keywordField("source"),
    intField("priority"),
    booleanField("complete")
  )
}
