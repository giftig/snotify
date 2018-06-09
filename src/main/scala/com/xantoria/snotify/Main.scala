package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.amqp.QueueDeclaration
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert._
import com.xantoria.snotify.api.{SourceStreamHandler, SourceStreaming}
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.persist.Persistence
import com.xantoria.snotify.rest.{Service => RestService}

object Main extends StrictLogging {
  lazy val persistHandler: Persistence = Config.persistHandler.newInstance match {
    case p: Persistence => p
    case _ => throw new IllegalArgumentException(
      s"Bad persistence class ${Config.persistHandler.getName}"
    )
  }

  def restApi(
    notificationSink: Sink[ReceivedNotification, NotUsed]
  )(implicit system: ActorSystem, mat: Materializer): RestService = {
    new RestService(Config.restInterface, Config.restPort, notificationSink)
  }

  def alertScheduler()(implicit system: ActorSystem, mat: Materializer): ActorRef = {
    import system.dispatcher

    val alertHandler = new RootAlertHandler
    system.actorOf(Props(new AlertScheduler(alertHandler, persistHandler, Config.alertingBackoff)))
  }

  def sourceHandler(
    alertScheduler: ActorRef
  )(implicit system: ActorSystem, mat: Materializer): SourceStreaming = {
    new SourceStreamHandler(alertScheduler, persistHandler, system, mat)
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val mat = ActorMaterializer()

    val scheduler = alertScheduler()
    val srcHandler = sourceHandler(scheduler)
    val rest = restApi(srcHandler.persistAndSchedule)

    srcHandler.runSources()
    rest.serve()
  }
}
