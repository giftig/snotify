package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.amqp.QueueDeclaration
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert._
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.dao._
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.rest.{Service => RestService}
import com.xantoria.snotify.streaming.{
  App => StreamingApp,
  NotificationReader,
  NotificationReading
}

object Main extends StrictLogging {
  lazy val notificationDao: Persistence = Config.persistHandler.newInstance match {
    case p: Persistence => p
    case _ => throw new IllegalArgumentException(
      s"Bad persistence class ${Config.persistHandler.getName}"
    )
  }

  def restApi(
    streamingDao: StreamingPersistence,
    scheduler: ActorRef
  )(implicit system: ActorSystem, mat: Materializer): RestService = {
    new RestService(Config.restInterface, Config.restPort, streamingDao, scheduler)
  }

  def alertScheduler(dao: Persistence)(
    implicit system: ActorSystem, mat: Materializer
  ): ActorRef = {
    val alertHandler = new RootAlertHandler
    system.actorOf(Props(new AlertScheduler(alertHandler, dao, Config.alertingBackoff)))
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val mat = ActorMaterializer()

    import system.dispatcher

    val streamingDao = new StreamingDao(notificationDao, Config.persistThreads)
    val scheduler = alertScheduler(notificationDao)
    val streamNotificationSource = new NotificationReader

    val streamingApp = new StreamingApp(scheduler, streamingDao, streamNotificationSource)
    val rest = new RestService(Config.restInterface, Config.restPort, streamingDao, scheduler)

    // TODO: Graceful shutdown?
    streamingApp.run()
    rest.serve()
  }
}
