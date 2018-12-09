package com.xantoria.snotify.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging

class Service(host: String, port: Int, override protected val notificationSink: ActorRef)(
  protected implicit val actorSystem: ActorSystem,
  override protected implicit val materializer: Materializer
) extends Routing with StrictLogging {
  def serve(): Unit = {
    logger.info(s"Serving HTTP API on $host:$port")
    Http().bindAndHandle(routes, host, port)
  }
}
