package com.xantoria.snotify.alert

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.stream.Materializer

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.alert.desktop._
import com.xantoria.snotify.alert.pushover._

trait RootAlertHandling extends AlertHandling {
  protected implicit val actorSystem: ActorSystem
  protected implicit val mat: Materializer

  private lazy val handlers: Seq[AlertHandling] = Seq(
    audioHandler,
    notifySendHandler,
    pushoverHandler
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

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    val results: Seq[Future[Boolean]] = handlers map { _.triggerAlert(n) }
    Future.sequence(results) map { !_.contains(false) }
  }
}

class RootAlertHandler(
  override implicit protected val actorSystem: ActorSystem,
  override implicit protected val mat: Materializer
) extends RootAlertHandling