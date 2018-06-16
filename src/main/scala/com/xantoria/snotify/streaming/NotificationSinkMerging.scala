package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream.scaladsl._

import com.xantoria.snotify.model.Notification

/**
 * A notification writer which passes notifications on to multiple sinks
 */
trait NotificationSinkMerging extends NotificationWriting {
  protected val writers: Seq[NotificationWriting]

  def sink(): Sink[Notification, NotUsed] = writers map { _.sink() } match {
    case Nil => Sink.ignore mapMaterializedValue { _ => NotUsed }
    case single :: Nil => single
    case first :: second :: remaining => {
      Sink.combine(first, second, remaining: _*)(Broadcast[Notification](_))
    }
  }
}
