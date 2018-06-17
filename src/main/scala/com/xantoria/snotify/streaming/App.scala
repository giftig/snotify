package com.xantoria.snotify.streaming

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.dao.StreamingPersistence
import com.xantoria.snotify.model.ReceivedNotification

class App(
  override protected val scheduler: ActorRef,
  override protected val streamingDao: StreamingPersistence,
  override protected val source: NotificationSource[ReceivedNotification]
)(
  override protected implicit val actorSystem: ActorSystem,
  override protected implicit val mat: Materializer
) extends NotificationStreaming with StrictLogging {
  def run(): ActorRef = {
    logger.info("Running notification reader graph")
    graph.run()
  }
}
