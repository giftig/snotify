package com.xantoria.snotify.streaming

import org.joda.time.DateTime

import com.xantoria.snotify.model._

class TestNotification(override val notification: Notification) extends ReceivedNotification {
  import TestNotification._

  var state: State = Unacked

  override def ack(): Unit = state = Acked
  override def reject(): Unit = state = Rejected
  override def retry(): Unit = state = Retried
}

object TestNotification {
  val SelfTarget = "test-selftarget"
  val Peers = Set("test-peertarget-1", "test-peertarget-2", "test-peertarget-3")
  val UnknownTarget = "test-unknown-target"

  sealed trait State
  case object Acked extends State
  case object Rejected extends State
  case object Retried extends State
  case object Unacked extends State

  /**
   * Create a test notification with a handful of useful properties configurable, others defaulted
   */
  def apply(
    id: String = "hodorhodor",
    targets: Seq[String] = Seq(SelfTarget),
    triggerTime: DateTime = DateTime.now,
    complete: Boolean = false
  ): TestNotification = new TestNotification(Notification(
    id = id,
    body = "(message body)",
    title = Some("(title)"),
    targets = targets,
    triggerTime = triggerTime,
    creationTime = Some(DateTime.now),
    source = Some("TestNotification"),
    priority = Priority.Medium,
    complete = complete
  ))
}
