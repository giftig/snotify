package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.QueueDeclaration
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.queue.QueueHandler

object Main extends StrictLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val materialiser = ActorMaterializer()

    val queues = new QueueHandler
    val src = queues.source()

    // FIXME: temp
    val testSink = Sink.foreach {
      n: ReceivedNotification => logger.info(s"Received message: ${n.notification}")
    }
    val result = src runWith testSink
  }
}
