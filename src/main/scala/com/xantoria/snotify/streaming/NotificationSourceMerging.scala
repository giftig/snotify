package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl._

import com.xantoria.snotify.model.ReceivedNotification

/**
 * Read notifications from several stream sources provided via other NotificationSource instances
 *
 * Those instances are instantiated from config by classname, combined into one source, and then
 * provided as a single source with some supervision applied. This will then be fed into the
 * notification processing graph
 */
trait NotificationSourceMerging extends NotificationSource[ReceivedNotification] {
  protected def readers: Seq[NotificationSource[ReceivedNotification]] = {
    NotificationSource.configuredReaders
  }

  override def source(): Source[ReceivedNotification, NotUsed] = {
    readers map { _.source() } reduce { _.merge(_, eagerComplete = true) }
  }
}
