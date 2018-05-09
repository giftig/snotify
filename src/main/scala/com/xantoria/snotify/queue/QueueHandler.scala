package com.xantoria.snotify.queue

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import com.xantoria.snotify.api.NotificationSource
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._

trait QueueHandling extends NotificationSource[AMQPNotification] {
  protected val input: QueueDeclaration
  protected val peers: Map[String, QueueDeclaration]
}

class QueueHandler extends QueueHandling with StrictLogging {
  private lazy val conn: AmqpConnectionProvider = {
    logger.info(s"Connecting to AMQ at ${Config.amqInterface}")
    AmqpUriConnectionProvider(Config.amqInterface)
  }

  override protected val input: QueueDeclaration = QueueDeclaration(Config.inputQueue)
  override protected val peers: Map[String, QueueDeclaration] = Config.peerQueues mapValues {
    q => QueueDeclaration(q)
  }

  override def source(): Source[AMQPNotification, NotUsed] = {
    logger.info(s"Consuming from ${input.name}")

    val raw = AmqpSource.committableSource(
      NamedQueueSourceSettings(conn, input.name, Seq(input)),
      Config.amqInputBufferSize
    )
    raw map {
      msg: CommittableIncomingMessage => {
        val encoding = Option(msg.message.properties.getContentEncoding) getOrElse "utf-8"
        val rawData = msg.message.bytes.decodeString(encoding)
        val n = rawData.parseJson.convertTo[Notification]
        AMQPNotification(n, msg)
      }
    }
  }
}
