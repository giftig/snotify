package com.xantoria.snotify.queue

import scala.util.control.NonFatal

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._
import com.xantoria.snotify.streaming.NotificationSource

trait QueueHandling extends NotificationSource[AMQPNotification] with StrictLogging {
  protected val input: QueueDeclaration
  protected val amqInterface: String

  private lazy val conn: AmqpConnectionProvider = {
    logger.info(s"Connecting to AMQ at $amqInterface")
    AmqpUriConnectionProvider(amqInterface)
  }

  override def source(): Source[AMQPNotification, NotUsed] = {
    logger.info(s"Consuming from ${input.name}")

    val raw = AmqpSource.committableSource(
      NamedQueueSourceSettings(conn, input.name, Seq(input)),
      Config.amqInputBufferSize  // FIXME: Pass this in
    )
    raw map {
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

class QueueHandler(
  override protected val amqInterface: String,
  inputQueueName: String
) extends QueueHandling {
  override protected val input: QueueDeclaration = QueueDeclaration(inputQueueName)
}
