package com.xantoria.snotify.alert

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification

class LoggingAlertHandler extends AlertHandling with StrictLogging {
  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Received message: $n")
    Future.successful(true)
  }
}
