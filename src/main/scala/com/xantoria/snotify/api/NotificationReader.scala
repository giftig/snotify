package com.xantoria.snotify.api

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer

import com.xantoria.snotify.persist.StreamingPersistence

class NotificationReader(
  override protected val scheduler: ActorRef,
  override protected val persistStreamer: StreamingPersistence,
  system: ActorSystem,
  materializer: Materializer
) extends NotificationReading {
  override protected implicit val actorSystem = system
  override protected implicit val mat = materializer
}
