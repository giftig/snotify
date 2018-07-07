package com.xantoria.snotify.streaming

import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.targeting.TargetResolution

class IncomingTargetResolver[T <: ReceivedNotification](
  override protected val resolver: TargetResolution
) extends IncomingTargetResolution[T]
