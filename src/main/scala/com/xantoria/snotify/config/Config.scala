package com.xantoria.snotify.config

import com.typesafe.config.{Config => TConfig, ConfigFactory}

object Config {
  private val cfg: TConfig = ConfigFactory.load()

  val amqInterface = {
    val amq = cfg.getConfig(s"amq")
    val host = amq.getString("host")
    val port = amq.getInt("port")
    s"tcp://$host:$port"
  }
}
