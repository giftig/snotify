package com.xantoria.snotify.config

import java.io.File
import scala.collection.JavaConverters._

import com.typesafe.config.{Config => TConfig, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.backoff.BackoffStrategy
import com.xantoria.snotify.targeting.TargetGroup

object Config extends StrictLogging {
  import ConfigHelpers._
  private val cfg: TConfig = {
    val default = ConfigFactory.load()

    val merged = Option(System.getProperty("app.config")) map { configFile =>
      logger.info(s"Using config from file $configFile")
      ConfigFactory.parseFile(new File(configFile)).withFallback(default)
    } getOrElse default

    merged.resolve()
  }
  private val amq: TConfig = cfg.getConfig("amq")
  private val persist: TConfig = cfg.getConfig("persist")
  private val rest: TConfig = cfg.getConfig("rest")

  // Clustering / target resolution config
  val clientId = cfg.getString("client-id")

  val peerIds: Set[String] = {
    val values = cfg.getStringList("cluster.peers").asScala.toSet
    if (values.contains(clientId)) {
      throw new IllegalArgumentException("Cannot configure myself to be my own peer!")
    }

    values
  }

  val targetGroups: Set[TargetGroup] = cfg.getConfigList("cluster.groups").asScala.toSet map {
    c: TConfig => c.toTargetGroup
  }

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
  val clusterInputQueue = queuePrefix
  val inputQueue = s"$queuePrefix-$clientId"
  val peerQueues = peerIds.map { s: String => (s, s"$queuePrefix-$s") }.toMap

  val amqInputBufferSize: Int = amq.getInt("buffer-size")

  val restInterface: String = rest.getString("interface")
  val restPort: Int = rest.getInt("port")
}
