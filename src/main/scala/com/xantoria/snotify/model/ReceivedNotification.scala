package com.xantoria.snotify.model

/**
 * A notification received from a particular source, which may require acknowledgement
 */
trait ReceivedNotification {
  val notification: Notification

  def ack(): Unit
  def reject(): Unit
  def retry(): Unit
}
