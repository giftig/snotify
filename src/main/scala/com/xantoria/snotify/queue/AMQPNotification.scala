package com.xantoria.snotify.queue

import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

case class AMQPNotification(
  notification: Notification,
  message: CommittableIncomingMessage
) extends ReceivedNotification {
  override def ack(): Unit = message.ack()
}
