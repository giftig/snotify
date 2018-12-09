package com.xantoria.snotify.alert.desktop

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.{Notification, Priority}
import com.xantoria.snotify.utils.PriorityTranslator

class NotifySendAlert extends AlertHandling with StrictLogging {
  import NotifySendAlert._

  private lazy val cfg = Config.alertingConfig.getConfig("notify-send")

  private lazy val icons: Map[String, String] = {
    val mapping = cfg.getConfig("icons")

    Seq(LowUrgency, NormalUrgency, CriticalUrgency).map { urg =>
      val v: Option[String] = if (mapping.hasPath(urg)) Some(mapping.getString(urg)) else None
      urg -> v
    }.collect {
      case (k, Some(v)) => k -> v
    }.toMap
  }

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    import scala.sys.process._
    logger.debug(s"Displaying notification ${n.id} with notify-send...")

    val urg: String = urgency(n.priority)
    val icon: Option[String] = icons.get(urg)

    // Patch for a bug in notify-send which causes it to not show messages
    // See www.archivum.info/ubuntu-bugs: Bug 1424243
    val body = n.body.replaceAll("&", "and")

    val cmd = {
      val base = Seq(
        "/usr/bin/env",
        "notify-send",
        "-t", "60000",  // TODO: configure duration
        "-u", urg
      )
      val iconArg = icon map { i => Seq("-i", i) } getOrElse Nil
      val ending = Seq(
        n.title getOrElse "(no title)",
        body
      )
      base ++: iconArg ++: ending
    }

    logger.debug(s"Running command: $cmd")
    Future(cmd ! ProcessLogger(_ => ())) map { _ == 0 }
  }
}

object NotifySendAlert {
  final val LowUrgency = "low"
  final val NormalUrgency = "normal"
  final val CriticalUrgency = "critical"

  // Translate internal priority into a notify-send "urgency" label
  private val urgency = new PriorityTranslator[String](Map(
    Priority.Medium -> LowUrgency,
    Priority.High -> NormalUrgency,
    Priority.Max -> CriticalUrgency
  ))
}
