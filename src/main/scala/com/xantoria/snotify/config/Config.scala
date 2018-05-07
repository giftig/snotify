package com.xantoria.snotify.config

import scala.collection.JavaConverters._

import com.typesafe.config.{Config => TConfig, ConfigFactory}

object Config {
  private val cfg: TConfig = ConfigFactory.load()
  private val amq: TConfig = cfg.getConfig("amq")

  val clientId = cfg.getString("client-id")
  val peerIds: Set[String] = cfg.getStringList("cluster.peers").asScala.toSet

  val amqInterface: String = {
    val protocol = amq.getString("protocol")
    val host = amq.getString("host")
    val port = amq.getInt("port")

    val auth = {
      val username = if (amq.hasPath("username")) Some(amq.getString("username")) else None
      val password = if (amq.hasPath("password")) Some(amq.getString("password")) else None

      (username, password) match {
        case (Some(u), Some(p)) => s"$u:$p@"
        case (Some(u), None) => s"$u@"
        case _ => ""
      }
    }

    // TODO: Construct this URI properly
    s"$protocol://$auth$host:$port"
  }

  private val queuePrefix = amq.getString("queue-prefix")
  val inputQueue = s"$queuePrefix-$clientId"
  val peerQueues = peerIds.map { s: String => (s, s"$queuePrefix-$s") }.toMap

  val amqInputBufferSize = amq.getInt("buffer-size")
}