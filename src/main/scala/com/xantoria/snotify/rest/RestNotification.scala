package com.xantoria.snotify.rest

import scala.concurrent.{ExecutionContext, Future, Promise}

import akka.actor.{ActorRef, ActorSystem}

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

case class RestNotification(override val notification: Notification) extends ReceivedNotification {
  import RestNotification._

  private val promise = Promise[Response]()
  val result: Future[Response] = promise.future

  override def ack(): Unit = promise.success(Acked)
  override def reject(): Unit = promise.failure(new RuntimeException("Notification rejected"))
  override def retry(): Unit = reject()

  override def stored(): Unit = promise.success(Stored)
  override def ignored(): Unit = promise.success(Ignored)
  override def forwarded(): Unit = promise.success(Forwarded)

  override def error(t: Throwable): Unit = promise.failure(t)
}

object RestNotification {
  sealed trait Response
  case object Acked extends Response
  case object Stored extends Response
  case object Ignored extends Response
  case object Forwarded extends Response
}
