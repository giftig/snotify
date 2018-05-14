package com.xantoria.snotify.alert

import scala.concurrent.{ExecutionContext, Future}

import com.xantoria.snotify.model.Notification

/**
 * An alert handler which concurrently runs multiple other alert handlers
 */
class MultipleAlertHandler(handlers: Seq[AlertHandling]) extends AlertHandling {
  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    val results: Seq[Future[Boolean]] = handlers map { _.triggerAlert(n) }
    Future.sequence(results) map { !_.contains(false) }
  }
}
