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
    logger.warn(s"Rejected notification ${notification.id}")
    message.nack(requeue = false)
  }
  override def retry(): Unit = {
    logger.warn(s"Requeued notification ${notification.id}")
    message.nack(requeue = true)
  }
}
