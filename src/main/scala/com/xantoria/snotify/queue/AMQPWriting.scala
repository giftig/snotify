package com.xantoria.snotify.queue

import scala.concurrent.duration._

import akka.NotUsed
import akka.stream.scaladsl._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._
import com.xantoria.snotify.streaming.NotificationWriting

trait AMQPWriting extends NotificationWriting with AMQPConnectionMgmt with StrictLogging {
  protected val output: QueueDeclaration

  private lazy val amqpSink: Sink[OutgoingMessage, NotUsed] = {
    val base =AmqpSink(
      AmqpSinkSettings(amqpConnection)
        .withRoutingKey(output.name)
        .withDeclarations(output)
    ) mapMaterializedValue { _ => NotUsed }

    RestartSink.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 15.seconds,
      randomFactor = 0.2,
      maxRestarts = 15
    ) { () => base }
  }

  /**
   * Turn a Notification into an OutgoingMessage (wrapping a raw ByteString)
   */
  private def writeMessage(n: Notification): OutgoingMessage = {
    val rawData = ByteString(n.toJson.compactPrint)
    new OutgoingMessage(bytes = rawData, immediate = false, mandatory = true)
  }

  /**
   * Filter notifications to those which belong on this queue (according to config) and queue them
   */
  def sink(): Sink[Notification, NotUsed] = {
    Flow[Notification].map { n: Notification =>
      logger.info(s"Writing notification ${n.id} to AMQP (queue ${output.name})")
      writeMessage(n)
    }.to(amqpSink)
  }
}
