package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl.Source

class TestNotificationSource(
  notifications: Seq[TestNotification]
) extends NotificationSource[TestNotification] {
  def source(): Source[TestNotification, NotUsed] = Source(notifications.toList)
}
