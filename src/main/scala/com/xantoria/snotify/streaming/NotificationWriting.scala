package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification

trait NotificationWriting extends StrictLogging {
  def sink(): Sink[Notification, NotUsed]
}
