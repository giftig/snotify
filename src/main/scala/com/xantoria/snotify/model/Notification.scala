package com.xantoria.snotify.model

import java.util.UUID

import org.joda.time.DateTime

/**
 * Stub implementation for a notification message
 */
case class Notification(
  id: String,
  body: String,
  title: Option[String],
  targets: Seq[String],
  triggerTime: DateTime,
  creationTime: Option[DateTime],
  source: Option[String]
)

object Notification {
  val MaxBodyLen: Int = 2048
  val MaxTitleLen: Int = 256
  val MaxTargetLen: Int = 32

  def id: String = UUID.randomUUID.toString
}
