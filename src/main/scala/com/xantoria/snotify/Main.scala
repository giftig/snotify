package com.xantoria.snotify

import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.config.Config

object Main extends StrictLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting service snotify...")
    logger.info(s"Using AMQ at ${Config.amqInterface}")
  }
}
