package com.xantoria.snotify.dao.elasticsearch

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}

import com.sksamuel.elastic4s.UnparsedElasticDate
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
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
  override def init()(implicit ec: ExecutionContext): Future[Unit] = {
    val q = createIndex(indexName).mappings(Mappings.Notification)
    val res = client.execute(q)
    res map {
      case Right(_) => ()
      case Left(f) if f.error.`type` == IndexAlreadyExistsException => ()
      case Left(f) => throw new IllegalStateException(
        s"Unexpected error initialising elasticsearch: ${f.error}"
      )
    }
  }

  override def save(n: Notification)(implicit ec: ExecutionContext): Future[WriteResult] = {
    val q = indexInto(indexName / NotificationType).createOnly(true).doc(n).id(n.id)
    client.execute(q) map {
      case Right(res) => Inserted
      case Left(f) if f.error.`type` == VersionConflict => Ignored
      case Left(f) => throw new ElasticsearchException(f)
    }
  }

  /**
   * Find notifications which are not yet complete and trigger in the next week
   */
  override def findPending()(implicit ec: ExecutionContext): Future[Seq[Notification]] = {
    val q = searchWithType(indexName -> NotificationType) bool {
      must(
        rangeQuery("trigger_time") lte UnparsedElasticDate("now+1w"),
        termQuery("complete", false)
      )
    }

    mapResponse(client.execute(q)) { res => res.result.hits.hits map { _.to[Notification] } }
  }

  /**
   * Mark the specified notification as complete
   */
  override def markComplete(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = updateById(indexName, NotificationType, n.id).script(MarkCompleteScript)

    mapResponse(client.execute(q)) { _ => () }
  }

  /**
   * Mark the specified notification as undeliverable
   */
  override def markFailed(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = ???
}

object ESHandling {
  val NotificationType: String = "notification"
  val IndexAlreadyExistsException: String = "index_already_exists_exception"
  val VersionConflict: String = "version_conflict_engine_exception"

  // Script used with the update API to mark a notification complete
  val MarkCompleteScript = "ctx._source.complete = true"

  private class ElasticsearchException(f: RequestFailure) extends RuntimeException(
    s"Elasticsearch request failed: ${f.error}"
  )

  /**
   * Convenience method to take the elasticsearch response (from client.execute) and map a success
   */
  def mapResponse[T, U](
    res: Future[Either[RequestFailure, RequestSuccess[U]]]
  )(f: RequestSuccess[U] => T)(implicit ec: ExecutionContext): Future[T] = res map {
    case Right(success) => f(success)
    case Left(failure) => throw new ElasticsearchException(failure)
  }
}
