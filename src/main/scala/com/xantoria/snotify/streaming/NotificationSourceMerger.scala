package com.xantoria.snotify.streaming

import com.xantoria.snotify.model.ReceivedNotification

class NotificationSourceMerger(
  override protected val readers: Seq[NotificationSource[ReceivedNotification]]
) extends NotificationSourceMerging
