package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.utils.Targeted

trait TargetedWriting extends NotificationWriting with Targeted {
  protected val underlying: NotificationWriting

  def sink(): Sink[Notification, NotUsed] = {
    Flow[Notification].filter(filterByTarget _).to(underlying.sink())
  }
}
