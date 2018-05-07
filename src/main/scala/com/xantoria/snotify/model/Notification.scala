package com.xantoria.snotify.model

/**
 * Stub implementation for a notification message
 *
 * TODO: Needs to be fleshed out
 */
case class Notification(
  body: String,
  title: Option[String],
  targets: Seq[String]
)
