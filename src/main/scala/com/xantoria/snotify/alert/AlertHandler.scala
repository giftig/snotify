package com.xantoria.snotify.alert

import akka.actor._

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

class AlertHandler extends Actor with LoggingAlertHandling {
  def receive: Receive = {
    case n: ReceivedNotification => triggerAlert(n.notification)
  }
}
