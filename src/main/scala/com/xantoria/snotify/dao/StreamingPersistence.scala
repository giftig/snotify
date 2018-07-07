package com.xantoria.snotify.dao

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

import akka.NotUsed
import akka.stream.scaladsl._

import com.xantoria.snotify.model.{Notification, ReceivedNotification}

// TODO: make a streaming API the default API; this is going to be much better for findPending()
// especially, for example
trait StreamingPersistence {
  protected implicit val ec: ExecutionContext
  protected val persistThreads: Int
  protected val underlying: Persistence
  protected val refreshInterval: Option[FiniteDuration]

  /**
   * A source of notifications which have been previous persisted and are still pending
   */
  def persistedSource: Source[Notification, NotUsed] = {
    Source
      .lazilyAsync { () => underlying.findPending() }
      .mapConcat { _.toList }
      .mapMaterializedValue { _ => NotUsed }
  }

  /**
   * Run `persistedSource` indefinitely, rerunning the query every `refreshInterval`
   *
   * If `refreshInterval` isn't provided, we'll just run it once, i.e. return `persistedSource`
   */
  def periodicSource: Source[Notification, NotUsed] = {
    refreshInterval map { period =>
      Source
        .tick(Duration.Zero, period, persistedSource)
        .flatMapConcat(identity)
        .mapMaterializedValue { _ => NotUsed }
    } getOrElse persistedSource
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
