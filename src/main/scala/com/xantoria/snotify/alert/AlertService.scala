package com.xantoria.snotify.alert

import akka.actor._

import com.xantoria.snotify.model.{Notification, ReceivedNotification}
import com.xantoria.snotify.utils.{Time => TimeUtils}

class AlertService(handler: AlertHandling) extends Actor {
  import AlertService._
  import context.dispatcher

  // TODO: Consider akka-quartz-scheduler
  def receive: Receive = {
    case n: ReceivedNotification => context.system.scheduler.scheduleOnce(
      TimeUtils.timeUntil(n.notification.triggerTime),
      self,
      TriggerAlert(n.notification)
    )
    case TriggerAlert(n: Notification) => handler.triggerAlert(n)
  }
}

object AlertService {
  case class TriggerAlert(n: Notification)
}
