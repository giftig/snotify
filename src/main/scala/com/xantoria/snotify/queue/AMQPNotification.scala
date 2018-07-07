package com.xantoria.snotify.queue

import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

case class AMQPNotification(
  notification: Notification,
  message: CommittableIncomingMessage
) extends ReceivedNotification with StrictLogging {
  override def ack(): Unit = message.ack()
  override def reject(): Unit = {
    logger.warn(s"Rejected $this")
    message.nack(requeue = false)
  }
  override def retry(): Unit = {
    logger.warn(s"Requeued $this")
    message.nack(requeue = true)
  }

  override def error(t: Throwable): Unit = retry()
}
