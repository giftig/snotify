package com.xantoria.snotify.config

import scala.collection.JavaConverters._

import com.typesafe.config.{Config => TConfig, ConfigFactory}

import com.xantoria.snotify.backoff.BackoffStrategy

object Config {
  import ConfigHelpers._
  private val cfg: TConfig = ConfigFactory.load()
  private val amq: TConfig = cfg.getConfig("amq")
  private val persist: TConfig = cfg.getConfig("persist")

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

  val notificationReaders: Seq[Class[_]] = cfg.getStringList("readers").asScala map {
    c => Class.forName(c)
  }

  val persistHandler: Class[_] = Class.forName(persist.getString("class"))
  val persistThreads: Int = persist.getInt("threads")
  val persistConfig: TConfig = persist.getConfig("config")  // storage-specific config

  val alertingConfig: TConfig = cfg.getConfig("alerting")
  val alertingBackoff: BackoffStrategy = alertingConfig.getBackoffStrategy("backoff-strategy")

  private val queuePrefix = amq.getString("queue-prefix")
  val inputQueue = s"$queuePrefix-$clientId"
  val peerQueues = peerIds.map { s: String => (s, s"$queuePrefix-$s") }.toMap

  val amqInputBufferSize = amq.getInt("buffer-size")
}
