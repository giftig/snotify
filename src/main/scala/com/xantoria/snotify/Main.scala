package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.QueueDeclaration
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config

object Main extends StrictLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val materialiser = ActorMaterializer()

    val queues = new QueueHandler
    val consumer = queues.consume()

    // FIXME: temp
    val testSink = Sink.foreach { s: String => logger.info(s"Received message: $s") }
    val result = consumer map { _.message.bytes.toString } runWith testSink

    import system.dispatcher
    result onComplete {
      case Success(_) => logger.info("Stream completed successfully")
      case Failure(t: Throwable) => logger.error("Ruh roh", t)
    }
  }
}
