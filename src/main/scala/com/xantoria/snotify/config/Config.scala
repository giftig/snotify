package com.xantoria.snotify.config

import scala.collection.JavaConverters._

import com.typesafe.config.{Config => TConfig, ConfigFactory}

object Config {
  private val cfg: TConfig = ConfigFactory.load()

  val clientId = cfg.getString("client-id")
  val peerIds: Set[String] = cfg.getStringList("cluster.peers").asScala.toSet

  val amqInterface: String = {
    val amq = cfg.getConfig("amq")
    val host = amq.getString("host")
    val port = amq.getInt("port")
    s"tcp://$host:$port"
  }

  private val queuePrefix = cfg.getString("amq.queue-prefix")
  val inputQueue = s"$queuePrefix-$clientId"
  val peerQueues = peerIds.map { s: String => (s, s"") }.toMap

}
