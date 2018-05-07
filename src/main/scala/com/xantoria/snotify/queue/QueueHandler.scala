package com.xantoria.snotify

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config

trait QueueHandling {
  protected val input: QueueDeclaration
  protected val peers: Map[String, QueueDeclaration]

  def consume(): Source[CommittableIncomingMessage, NotUsed]
}

class QueueHandler extends QueueHandling with StrictLogging {
  private lazy val conn: AmqpConnectionProvider = {
    logger.info(s"Connecting to AMQ at ${Config.amqInterface}")
    AmqpUriConnectionProvider(Config.amqInterface)
  }

  protected val input: QueueDeclaration = QueueDeclaration(Config.inputQueue)
  protected val peers: Map[String, QueueDeclaration] = Config.peerQueues mapValues {
    q => QueueDeclaration(q)
  }

  def consume(): Source[CommittableIncomingMessage, NotUsed] = {
    logger.info(s"Consuming from ${input.name}")
    AmqpSource.committableSource(
      NamedQueueSourceSettings(conn, input.name, Seq(input)),
      Config.amqInputBufferSize
    )
  }
}
