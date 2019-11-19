package com.xantoria.snotify.alert.pushover

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification

import JsonProtocol._

/**
 * Use the pushover API (pushover.net) to send notifications to mobile devices
 */
class PushoverAlert(
  implicit actorSystem: ActorSystem,
  mat: Materializer
) extends AlertHandling with SprayJsonSupport with StrictLogging {
  private lazy val cfg = Config.alertingConfig.getConfig("pushover")
  private lazy val url = cfg.getString("url")
  private lazy val token = cfg.getString("token")
  private lazy val apiUser = cfg.getString("user")

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Sending notification ${n.id} to Pushover")

    val msg = PushoverMessage(n, token, apiUser)
    logger.debug(s"Sending to Pushover: $msg")

    val entity: Future[RequestEntity] = Marshal(msg).to[RequestEntity]
    val req = entity map { e => HttpRequest(method = HttpMethods.POST, uri = url, entity = e) }

    // TODO: Apply (gentle) retries. Can probably do this with a proper streaming mechanism
    val resp: Future[HttpResponse] = req flatMap { r => Http().singleRequest(r) }
    resp foreach { _.discardEntityBytes() }

    resp map {
      case r if r.status.isSuccess =>
        logger.info(s"Delivered notification ${n.id} successfully")
        true
      case r =>
        logger.error(s"Got HTTP ${r.status.intValue} from Pushover")
        logger.debug(s"Full request: $req; response: $r")
        false
    } recover {
      case NonFatal(e) =>
        logger.error(s"Unexpected error delivering ${n.id}", e)
        false
    }
  }
}
