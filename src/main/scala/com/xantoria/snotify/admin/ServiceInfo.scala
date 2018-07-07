package com.xantoria.snotify.admin

import java.lang.management.ManagementFactory

import com.xantoria.snotify.config.Config

/**
 * Provides basic info about what the service is and what its current state is
 */
case class ServiceInfo(
  service: String = "snotify",
  version: String = "0.0.1-SNAPSHOT",  // TODO: Grab this from manifest etc
  client_id: String = Config.clientId,
  uptime: Long = ManagementFactory.getRuntimeMXBean.getUptime
)
