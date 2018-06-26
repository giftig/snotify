package com.xantoria.snotify.dao.elasticsearch

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.{Config => TConfig}

class ESStorage(cfg: TConfig) extends ESHandling {
  override protected val client: HttpClient = {
    val host = cfg.getString("host")
    val port = cfg.getInt("port")
    HttpClient(ElasticsearchClientUri(host, port))
  }
  override protected val indexName: String = cfg.getString("index")
}
