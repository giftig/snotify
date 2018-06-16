package com.xantoria.snotify

import scala.util.{Failure, Success}

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.amqp.{QueueDeclaration => Queue}
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert._
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.dao._
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.queue.{AMQPConnectionMgmt, AMQPReader, AMQPWriter}
import com.xantoria.snotify.rest.{Service => RestService}
import com.xantoria.snotify.streaming.{App => StreamingApp, _}

object Main extends StrictLogging {
  private lazy val notificationDao: Persistence = Config.persistHandler.newInstance match {
    case p: Persistence => p
    case _ => throw new IllegalArgumentException(
      s"Bad persistence class ${Config.persistHandler.getName}"
    )
  }

  // TODO: May need to connect to multiple hosts depending on the cluster
  private lazy val amqpConn = AMQPConnectionMgmt.connection(Config.amqInterface)

  private lazy val clusterHandler: NotificationSource[ReceivedNotification] = {
    val outputQueues = Config.peerQueues.toList map { case (pid, queueName) =>
      val q = Queue(queueName)
      val writer = new AMQPWriter(amqpConn, q)
      new TargetedWriter(writer, Set(pid))
    }

    new ClusterHandler(
      new AMQPReader(amqpConn, Queue(Config.inputQueue), Config.amqInputBufferSize),
      new AMQPReader(amqpConn, Queue(Config.clusterInputQueue), Config.amqInputBufferSize),
      new NotificationSinkMerger(outputQueues),
      Config.clientId,
      Config.peerIds.toSet
    )
  }

  private lazy val allSources: NotificationSource[ReceivedNotification] = {
    new NotificationSourceMerger(clusterHandler +: NotificationSource.configuredReaders)
  }

  private def restApi(
    streamingDao: StreamingPersistence,
    scheduler: ActorRef
  )(implicit system: ActorSystem, mat: Materializer): RestService = {
    new RestService(Config.restInterface, Config.restPort, streamingDao, scheduler)
  }

  private def alertScheduler(dao: Persistence)(
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

    val streamingApp = new StreamingApp(scheduler, streamingDao, allSources)
    val rest = new RestService(Config.restInterface, Config.restPort, streamingDao, scheduler)

    // TODO: Graceful shutdown?
    streamingApp.run()
    rest.serve()
  }
}
