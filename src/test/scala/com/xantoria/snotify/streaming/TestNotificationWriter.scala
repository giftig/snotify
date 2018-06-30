package com.xantoria.snotify.streaming

import scala.collection.mutable.MutableList

import akka.NotUsed
import akka.stream.scaladsl.Sink

import com.xantoria.snotify.model.Notification

class TestNotificationWriter extends NotificationWriting {
  var notifications: MutableList[Notification] = MutableList()

  def sink(): Sink[Notification, NotUsed] = {
    Sink.foreach { n: Notification => notifications += n } mapMaterializedValue { _ => NotUsed }
  }
}
