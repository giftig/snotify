package com.xantoria.snotify

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.amqp.{QueueDeclaration => Queue}
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config => TConfig}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert._
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.dao._
import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.queue.{AMQPConnectionMgmt, AMQPReader, AMQPWriter}
import com.xantoria.snotify.rest.{Service => RestService}
import com.xantoria.snotify.streaming.{App => StreamingApp, _}
import com.xantoria.snotify.targeting.{TargetGroup, TargetResolver}

object Main extends StrictLogging {
  private lazy val notificationDao: Persistence = {
    val constructor = Config.persistHandler.getConstructor(classOf[TConfig])

    constructor.newInstance(Config.persistConfig) match {
      case p: Persistence => p
      case _ => throw new IllegalArgumentException(
        s"Bad persistence class ${Config.persistHandler.getName}"
      )
    }
  }

  // TODO: May need to connect to multiple hosts depending on the cluster
  private lazy val amqpConn = AMQPConnectionMgmt.connection(Config.amqInterface)

  // Route all configured notification sources into the cluster handler first
  private lazy val clusterHandler: ClusterHandling[ReceivedNotification] = {
    val outputQueues = Config.peerQueues.toList map { case (pid, queueName) =>
      val q = Queue(queueName)
      val writer = new AMQPWriter(amqpConn, q)
      val names: Set[String] = Config.targetGroups.collect {
        case TargetGroup(name, members) if members.contains(pid) => name
      } + pid

      new TargetedWriter(writer, names)
    }
    val clusterQueue = new AMQPReader(
      amqpConn,
      Queue(Config.clusterInputQueue),
      Config.amqInputBufferSize
    )

    new ClusterHandler(
      new NotificationSourceMerger(clusterQueue +: NotificationSource.configuredReaders),
      new NotificationSinkMerger(outputQueues),
      new IncomingTargetResolver[ReceivedNotification](TargetResolver(
        Config.clientId,
        Config.peerIds,
        Config.targetGroups
      ))
    )
  }

  private lazy val personalQueue: NotificationSource[ReceivedNotification] = new AMQPReader(
    amqpConn,
    Queue(Config.inputQueue),
    Config.amqInputBufferSize
  )

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

    // Block until the db is ready; we can't do anything until then anyway
    Await.result(notificationDao.init(), 30.seconds)

    val streamingDao = new StreamingDao(
      notificationDao,
      Config.persistThreads,
      Config.dbRefreshInterval
    )
    val scheduler = alertScheduler(notificationDao)

    // TODO: Graceful shutdown?
    val streamingApp = new StreamingApp(scheduler, streamingDao, clusterHandler, personalQueue)
    val notificationHook: ActorRef = streamingApp.run()

    val rest = new RestService(Config.restInterface, Config.restPort, notificationHook)
    rest.serve()
  }
}
