package com.xantoria.snotify.api

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer

import com.xantoria.snotify.persist.Persistence

class SourceStreamHandler(
  protected val scheduler: ActorRef,
  protected val persistHandler: Persistence,
  system: ActorSystem,
  materializer: Materializer
) extends SourceStreaming {
  override protected implicit val actorSystem = system
  override protected implicit val mat = materializer
}
