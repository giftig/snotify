package com.xantoria.snotify.streaming

class TargetedWriter(
  override protected val underlying: NotificationWriting,
  override protected val targets: Set[String]
) extends TargetedWriting
