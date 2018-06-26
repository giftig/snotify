package com.xantoria.snotify.dao.elasticsearch

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.dao.Persistence
import com.xantoria.snotify.model.Notification

import IndexingProtocol._
import Persistence._

trait ESHandling extends Persistence with StrictLogging {
  import ESHandling._

  protected val client: HttpClient
  protected val indexName: String

  /**
   * Ensure the appropriate index gets created if necessary
   */
  protected def bootstrap()(implicit ec: ExecutionContext): Future[Unit] = {
    val q = createIndex(indexName)
    val res = client.execute(q)
    res map { _ => () } // TODO: recover conflicts etc
  }

  override def save(n: Notification)(implicit ec: ExecutionContext): Future[WriteResult] = {
    val q = indexInto(indexName / NotificationType).doc(n).id(n.id)
    client.execute(q) map {
      case Right(_) => Inserted
      case Left(failure) => throw new RuntimeException(failure.toString) // FIXME
    }
  }

  /**
   * Find notifications which are not yet complete
   */
  override def findPending()(implicit ec: ExecutionContext): Future[Seq[Notification]] = ???

  /**
   * Mark the specified notification as complete
   */
  override def markComplete(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = ???

  /**
   * Mark the specified notification as undeliverable
   */
  override def markFailed(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = ???
}

object ESHandling {
  val NotificationType: String = "notification"
}