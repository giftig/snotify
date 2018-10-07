package com.xantoria.snotify.alert.custom

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification

/**
 * Pass the notification details to a custom command specified in config
 *
 * This will probably be a script written to accept these arguments, wrapping a tool of your
 * choice. Arguments are provided as <id> <title> <body> <priority>
 */
class CustomAlert extends AlertHandling with StrictLogging {
  private lazy val cfg = Config.alertingConfig.getConfig("custom-command")
  private lazy val command: String = cfg.getString("command")

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    import scala.sys.process._
    logger.debug(s"Running custom command $command for notification ${n.id}...")

    val cmd = Seq(command, n.id, n.title getOrElse "", n.body, n.priority.toString)
    Future(cmd ! ProcessLogger(_ => ())) map { _ == 0 }
  }
}
