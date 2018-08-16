package com.xantoria.snotify.utils

import scala.collection.JavaConverters._

import com.typesafe.config.{ConfigObject, ConfigValue}

/**
 * Provides a generic means of converting a snotify priority integer into a named priority system
 *
 * For example, notify-send has a system like low < normal < critical, or pushover has a system
 * like -2 < -1 < 0 < 1 < 2
 */
class PriorityTranslator[T](thresholds: Map[Int, T]) {
  private lazy val sortedThresholds = thresholds.toList sortBy { _._1 }

  def apply(p: Int): T = {
    val (_, v) = sortedThresholds find {
      case (thresh, _) => thresh > p
    } getOrElse sortedThresholds.last

    v
  }
}

object PriorityTranslator {
  def fromConfig[T](cfg: ConfigObject)(parseConfig: ConfigValue => T): PriorityTranslator[T] = {
    val data: Map[Int, T] = cfg.asScala.toMap map {
      case (k, v) => (k.toInt, parseConfig(v))
    }
    new PriorityTranslator(data)
  }
}
