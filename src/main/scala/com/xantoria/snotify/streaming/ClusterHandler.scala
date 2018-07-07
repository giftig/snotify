package com.xantoria.snotify.streaming

import com.xantoria.snotify.model.ReceivedNotification

class ClusterHandler[T <: ReceivedNotification](
  override protected val notificationSource: NotificationSource[T],
  override protected val notificationWriter: NotificationWriting,
  override protected val targetResolver: IncomingTargetResolution[T]
) extends ClusterHandling[T]
