package com.xantoria.snotify.queue

import akka.NotUsed
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification

trait NotificationWriting extends StrictLogging {
  val sink: Sink[Notification, _] = Sink.foreach { n =>
    logger.warn(s"NOT IMPLEMENTED: not writing ${n.id} to peer queue") // FIXME
  }
}
