package com.xantoria.snotify.queue

import scala.concurrent.duration._
import scala.util.control.NonFatal

import akka.NotUsed
import akka.stream.scaladsl._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._
import com.xantoria.snotify.streaming.NotificationSource

trait AMQPReading extends NotificationSource[AMQPNotification]
  with AMQPConnectionMgmt
  with StrictLogging {

  protected val input: QueueDeclaration
  protected val bufferSize: Int

  override def source(): Source[AMQPNotification, NotUsed] = {
    logger.info(s"Consuming from ${input.name}")

    val raw = AmqpSource.committableSource(
      NamedQueueSourceSettings(amqpConnection, input.name, Seq(input)),
      bufferSize
    )
    val monitored = RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 15.seconds,
      randomFactor = 0.2,
      maxRestarts = 15
    ) { () => raw }

    monitored map {
      msg: CommittableIncomingMessage => {
        try {
          val encoding = Option(msg.message.properties.getContentEncoding) getOrElse "utf-8"
          val rawData = msg.message.bytes.decodeString(encoding)
          val n = rawData.parseJson.convertTo[Notification]
          AMQPNotification(n, msg)
        } catch {
          case NonFatal(t) => {
            logger.error("Error while deserialising message", t)
            msg.nack(requeue = false)
            throw t
          }
        }
      }
    }
  }
}
