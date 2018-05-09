package com.xantoria.snotify.api

import akka.NotUsed
import akka.stream.scaladsl.Source

import com.xantoria.snotify.model.ReceivedNotification

trait NotificationSource[+T <: ReceivedNotification] {
  def source(): Source[T, NotUsed]
}
