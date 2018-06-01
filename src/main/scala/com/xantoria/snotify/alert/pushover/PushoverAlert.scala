package com.xantoria.snotify.alert.pushover

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scala.dsl.Http
import akka.http.scala.dsl.model._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification

/**
 * Use the pushover API (pushover.net) to send notifications to mobile devices
 */
class PushoverAlert extends AlertHandling with StrictLogging {
  private lazy val cfg = Config.alertingConfig.getConfig("pushover")
  private lazy val url = cfg.getString("api.url")
  private lazy val token = cfg.getString("api.token")
  private lazy val apiUser = cfg.getString("api.user")

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Sending notification ${n.id} to Pushover")
    val msg = PushoverMessage(n, token, apiUser)
    logger.debug(s"Sending to Pushover: $msg")

    Http().singleRequest(HttpRequest(
      uri = url,

    ))
  }
}
