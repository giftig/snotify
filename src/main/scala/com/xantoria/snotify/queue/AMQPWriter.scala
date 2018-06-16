package com.xantoria.snotify.queue

import akka.stream.alpakka.amqp.{AmqpConnectionProvider, QueueDeclaration}

class AMQPWriter(
  override protected val amqpConnection: AmqpConnectionProvider,
  override protected val output: QueueDeclaration
) extends AMQPWriting
