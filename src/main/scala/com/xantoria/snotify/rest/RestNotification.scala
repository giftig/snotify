package com.xantoria.snotify.rest

import scala.concurrent.{ExecutionContext, Future, Promise}

import akka.actor.{ActorRef, ActorSystem}

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

case class RestNotification(override val notification: Notification) extends ReceivedNotification {
  private val promise = Promise[Unit]()
  val result: Future[Unit] = promise.future

  def ack(): Unit = promise.success(())
  def reject(): Unit = promise.failure(new RuntimeException("Notification processing failed"))
  def retry(): Unit = reject()
}
