package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.amqp.QueueDeclaration
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert._
import com.xantoria.snotify.api.SourceStreamHandler
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.persist.Persistence

object Main extends StrictLogging {
  lazy val persistHandler: Persistence = Config.persistHandler.newInstance match {
    case p: Persistence => p
    case _ => throw new IllegalArgumentException(
      s"Bad persistence class ${Config.persistHandler.getName}"
    )
  }

  def runSources()(implicit system: ActorSystem, mat: Materializer): Unit = {
    import system.dispatcher

    val alertHandler = new RootAlertHandler
    val alertScheduler: ActorRef = system.actorOf(
      Props(new AlertScheduler(alertHandler, persistHandler))
    )
    val streamHandler = new SourceStreamHandler(alertScheduler, persistHandler, system, mat)
    streamHandler.runSources()
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val mat = ActorMaterializer()
    runSources()
  }
}
