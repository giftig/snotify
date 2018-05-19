package com.xantoria.snotify.alert.desktop

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.{Config => TypesafeConfig}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.{Notification, Priority}

class NotifySendAlert extends AlertHandling with StrictLogging {
  private lazy val cfg = Config.alertingConfig.getConfig("notify-send")

  private lazy val icons: Map[String, String] = {
    val mapping = cfg.getConfig("icons")

    urgencyThresholds.map { case (k, _) =>
      val v: Option[String] = if (mapping.hasPath(k)) Some(mapping.getString(k)) else None
      k -> v
    }.collect {
      case (k, Some(v)) => k -> v
    }.toMap
  }

  // Urgency is the given value if priority is < the specified threshold. Must be ascending order
  private val urgencyThresholds: Seq[(String, Int)] = Seq(
    "low" -> Priority.Medium,
    "normal" -> Priority.High,
    "critical" -> Priority.Max
  )

  /**
   * Maps notification priority to notify-send "urgency"
   */
  private def urgency(p: Int): String = {
    val (urg, _) = urgencyThresholds find {
      case (name, maxValue) => maxValue > p
    } getOrElse urgencyThresholds.last

    urg
  }

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    import scala.sys.process._
    logger.info(s"Displaying notification ${n.id} with notify-send...")

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
        n.title getOrElse "(untitled notification)",
        body
      )
      base ++: iconArg ++: ending
    }

    logger.debug(s"Running command: $cmd")
    Future(cmd ! ProcessLogger(_ => ())) map { _ == 0 }
  }
}
