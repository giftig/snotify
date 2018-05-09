package com.xantoria.snotify.alert

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification

trait LoggingAlertHandling extends AlertHandling with StrictLogging {
  def triggerAlert(n: Notification): Boolean = {
    logger.info(s"Received message: $n")
    true
  }
}
