package com.xantoria.snotify.queue

import akka.stream.alpakka.amqp.{AmqpConnectionProvider, QueueDeclaration}

class AMQPReader(
  override protected val amqpConnection: AmqpConnectionProvider,
  override protected val input: QueueDeclaration,
  override protected val bufferSize: Int
) extends AMQPReading
