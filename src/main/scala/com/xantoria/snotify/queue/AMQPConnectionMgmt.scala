package com.xantoria.snotify.queue

import akka.stream.alpakka.amqp.{AmqpConnectionProvider, AmqpUriConnectionProvider}
import com.typesafe.scalalogging.StrictLogging

/**
 * Represents something which required an AMQP connection via alpakka
 */
trait AMQPConnectionMgmt {
  protected val amqpConnection: AmqpConnectionProvider
}

object AMQPConnectionMgmt extends StrictLogging {
  def connection(interface: String): AmqpConnectionProvider = {
    logger.info(s"Connecting to AMQP service at $interface")
    AmqpUriConnectionProvider(interface)
  }
}
