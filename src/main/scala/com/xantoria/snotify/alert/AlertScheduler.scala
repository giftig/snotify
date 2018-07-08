package com.xantoria.snotify.alert

import akka.actor._

import com.xantoria.snotify.backoff.{BackoffStrategy, ExponentialBackoffStrategy}
import com.xantoria.snotify.dao.Persistence
import com.xantoria.snotify.model.Notification

class AlertScheduler(
  override protected val alertHandler: AlertHandling,
  override protected val notificationDao: Persistence,
  override protected val backoffStrategy: BackoffStrategy = new ExponentialBackoffStrategy()
) extends Actor with AlertScheduling {
  import AlertScheduling._
  override protected val alertTarget = self

  def receive: Receive = {
    case n: Notification => scheduleNotification(n)
    case TriggerAlert(n: Notification, attempt: Int) => triggerAlert(n, attempt)
  }
}
