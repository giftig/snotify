package com.xantoria.snotify.dao.elasticsearch

import akka.actor.ActorSystem

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.akka.{AkkaHttpClient, AkkaHttpClientSettings}
import com.typesafe.config.{Config => TConfig}

class ESStorage(cfg: TConfig, system: ActorSystem) extends ESHandling {
  override protected val client: ElasticClient = {
    implicit val sys = system
    ElasticClient(AkkaHttpClient(AkkaHttpClientSettings(cfg)))
  }
  override protected val indexName: String = cfg.getString("index")
}
