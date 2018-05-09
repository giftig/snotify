package com.xantoria.snotify.api

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer

class SourceStreamHandler(
  protected val alertHandler: ActorRef,
  system: ActorSystem,
  materializer: Materializer
) extends SourceStreaming {
  override protected implicit val actorSystem = system
  override protected implicit val mat = materializer
}
