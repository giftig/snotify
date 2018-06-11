package com.xantoria.snotify.dao

import scala.concurrent.ExecutionContext

class StreamingDao(
  override protected val underlying: Persistence,
  override protected val persistThreads: Int
)(override protected implicit val ec: ExecutionContext) extends StreamingPersistence

