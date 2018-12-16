package com.xantoria.snotify.alert.pushover

import com.xantoria.snotify.model.{Notification, Priority}
import com.xantoria.snotify.utils.{PriorityTranslator, Strings}

/**
 * Represents a message sent to the Pushover API to deliver a notification
 */
case class PushoverMessage(
  token: String,
  user: String,
  title: String,
  message: String,
  priority: Int,
  timestamp: String,
  retry: String = "30",
  expire: String = "600",
  device: Option[String] = None,
  sound: Option[String] = None,
  url: Option[String] = None,
  url_title: Option[String] = None
)

object PushoverMessage {
  // Pushover enforces these limits so we'll have to obey them
  private final val MaxTitleLen = 250
  private final val MaxMessageLen: Int = 1024

  private val priority = new PriorityTranslator[Int](Map(
    Priority.Low -> -2,
    Priority.Medium -> -1,
    Priority.High -> 0,
    Priority.Critical -> 1,
    Priority.Max -> 2
  ))

  def apply(n: Notification, token: String, user: String): PushoverMessage = {
    val title = n.title map { t => Strings.truncate(t, MaxTitleLen) } getOrElse "(no title)"
    val msg = Strings.truncate(n.body, MaxMessageLen)

    PushoverMessage(
      token,
      user,
      title,
      msg,
      priority(n.priority),
      (n.triggerTime.getMillis / 1000).toString
    )
  }
}
