package com.xantoria.snotify.streaming

import scala.concurrent.duration._

import akka.NotUsed
import akka.stream.scaladsl._

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.{Notification, ReceivedNotification}

/**
 * Read notifications from several stream sources provided via other NotificationSource instances
 *
 * Those instances are instantiated from config by classname, combined into one source, and then
 * provided as a single source with some supervision applied. This will then be fed into the
 * notification processing graph
 */
trait NotificationReading extends NotificationSource[ReceivedNotification] {
  private lazy val readers: Seq[NotificationSource[ReceivedNotification]] = {
    Config.notificationReaders map { c =>
      val reader = c.newInstance
      reader match {
        case r: NotificationSource[ReceivedNotification] => r
        case _ => throw new IllegalArgumentException(s"Bad notification reader class ${c.getName}")
      }
    }
  }

  override def source(): Source[ReceivedNotification, NotUsed] = {
    val merged = readers map { _.source() } reduce { _.merge(_) }

    RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 15.seconds,
      randomFactor = 0.2,
      maxRestarts = 15
    ) { () => merged }
  }
}
