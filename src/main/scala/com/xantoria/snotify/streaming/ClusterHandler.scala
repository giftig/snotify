package com.xantoria.snotify.streaming

import com.xantoria.snotify.model.ReceivedNotification

class ClusterHandler[T <: ReceivedNotification](
  override protected val personalReader: NotificationSource[T],
  override protected val clusterReader: NotificationSource[T],
  override protected val notificationWriter: NotificationWriting,
  override protected val selfTarget: String,
  override protected val peers: Set[String]
) extends ClusterHandling[T]
