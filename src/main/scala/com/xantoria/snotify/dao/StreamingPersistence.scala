package com.xantoria.snotify.dao

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.stream.scaladsl._

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

trait StreamingPersistence {
  protected implicit val ec: ExecutionContext
  protected val persistThreads: Int
  protected val underlying: Persistence

  /**
   * A source of notifications which have been previous persisted and are still pending
   */
  def persistedSource: Source[Notification, NotUsed] = {
    Source.fromFuture(underlying.findPending()) mapConcat { _.toList }
  }

  /**
   * A flow accepting notifications; persists them and feeds back state via ReceivedNotification
   */
  def persistFlow: Flow[ReceivedNotification, Notification, NotUsed] = {
    Flow[ReceivedNotification].mapAsync(persistThreads) { underlying.save(_) } collect {
      case Some(n) => n
    }
  }
}
