package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl.Source

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification

trait NotificationSource[+T <: ReceivedNotification] {
  def source(): Source[T, NotUsed]
}

object NotificationSource {
  lazy val configuredReaders: Seq[NotificationSource[ReceivedNotification]] = {
    Config.notificationReaders map { c =>
      val reader = c.newInstance
      reader match {
        case r: NotificationSource[ReceivedNotification] => r
        case _ => throw new IllegalArgumentException(s"Bad notification reader class ${c.getName}")
      }
    }
  }

}
