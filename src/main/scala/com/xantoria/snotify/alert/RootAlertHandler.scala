package com.xantoria.snotify.alert

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.alert.custom._
import com.xantoria.snotify.alert.desktop._
import com.xantoria.snotify.alert.pushover._

trait RootAlertHandling extends AlertHandling {
  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  protected lazy val handlers: Seq[AlertHandling] = Seq(
    audioHandler,
    notifySendHandler,
    pushoverHandler,
    customHandler
  ).flatten

  // TODO: DRY
  private lazy val audioHandler: Option[AudioAlert] = {
    if (Config.alertingConfig.getBoolean("audio.enabled")) Some(new AudioAlert) else None
  }

  private lazy val notifySendHandler: Option[NotifySendAlert] = {
    if (Config.alertingConfig.getBoolean("notify-send.enabled")) {
      Some(new NotifySendAlert)
    } else {
      None
    }
  }

  private lazy val pushoverHandler: Option[PushoverAlert] = {
    if (Config.alertingConfig.getBoolean("pushover.enabled")) Some(new PushoverAlert) else None
  }

  private lazy val customHandler: Option[CustomAlert] = {
    if (Config.alertingConfig.getBoolean("custom.enabled")) Some(new CustomAlert) else None
  }

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    val results: Seq[Future[Boolean]] = handlers map { _.triggerAlert(n) }
    Future.sequence(results) map { !_.contains(false) }
  }
}

class RootAlertHandler(
  override implicit protected val actorSystem: ActorSystem,
  override implicit protected val mat: Materializer
) extends RootAlertHandling with StrictLogging {
  logger.info(
    s"Registered handlers: [${handlers.map { _.getClass.getSimpleName }.mkString(", ")}]"
  )
}
