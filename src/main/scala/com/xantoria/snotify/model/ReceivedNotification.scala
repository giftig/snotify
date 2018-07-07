package com.xantoria.snotify.model

/**
 * A notification received from a particular source, which may require acknowledgement
 */
trait ReceivedNotification {
  val notification: Notification

  def ack(): Unit
  def reject(): Unit
  def retry(): Unit = reject()

  // Additional operations provide some extra granularity if supported by the subclass, and default
  // to being aliases for the simpler API for those which only need a simple ack or nack
  def stored(): Unit = ack()
  def ignored(): Unit = ack()
  def forwarded(): Unit = ack()

  def error(t: Throwable): Unit = reject()

  override def toString: String = s"${getClass.getSimpleName} ${notification.id}"
}
