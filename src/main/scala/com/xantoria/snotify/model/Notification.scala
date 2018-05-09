package com.xantoria.snotify.model

import java.util.UUID

/**
 * Stub implementation for a notification message
 *
 * TODO: Needs to be fleshed out
 */
case class Notification(
  id: String,
  body: String,
  title: Option[String],
  targets: Seq[String]
)

object Notification {
  def id: String = UUID.randomUUID.toString
}
