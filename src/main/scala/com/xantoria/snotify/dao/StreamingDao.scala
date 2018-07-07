package com.xantoria.snotify.dao

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class StreamingDao(
  override protected val underlying: Persistence,
  override protected val persistThreads: Int,
  override protected val refreshInterval: Option[FiniteDuration]
)(override protected implicit val ec: ExecutionContext) extends StreamingPersistence
