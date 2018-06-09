package com.xantoria.snotify.rest

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.persist.StreamingPersistence


class Service(
  host: String,
  port: Int,
  notificationDao: StreamingPersistence,
  scheduler: ActorRef
)(
  protected implicit val actorSystem: ActorSystem,
  override protected implicit val materializer: Materializer
) extends Routing with StrictLogging {
  override protected val notificationSink: Sink[ReceivedNotification, NotUsed] = {
    notificationDao.persistFlow.to(Sink.actorRef(scheduler, Done))
  }

  def serve(): Unit = {
    logger.info(s"Serving HTTP API on $host:$port")
    Http().bindAndHandle(routes, host, port)
  }
}
