package com.xantoria.snotify.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging

class Service(host: String, port: Int)(
  implicit system: ActorSystem,
  mat: Materializer
) extends Routing with StrictLogging {
  def serve(): Unit = {
    logger.info(s"Serving HTTP API on $host:$port")
    Http().bindAndHandle(routes, host, port)
  }
}
