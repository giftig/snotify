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
import com.xantoria.snotify.queue.QueueHandler

object Main extends StrictLogging {
  lazy val alertHandlers: Seq[AlertHandling] = {
    Config.alertHandlers map {
      c: Class[_] => c.newInstance match {
        case a: AlertHandling => a
        case _ => throw new IllegalArgumentException(s"Bad alert handling class ${c.getName}")
      }
    }
  }

  def runSources()(implicit system: ActorSystem, mat: Materializer): Unit = {
    import system.dispatcher

    val alertHandler = new MultipleAlertHandler(alertHandlers)
    val alertService: ActorRef = system.actorOf(Props(new AlertService(alertHandler)))
    val streamHandler = new SourceStreamHandler(alertService, system, mat)
    streamHandler.runSources()
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")

    implicit val system = ActorSystem("snotify")
    implicit val mat = ActorMaterializer()
    runSources()
  }
}
