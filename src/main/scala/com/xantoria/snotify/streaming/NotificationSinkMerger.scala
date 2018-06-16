package com.xantoria.snotify.streaming

class NotificationSinkMerger(
  override protected val writers: Seq[NotificationWriting]
) extends NotificationSinkMerging
