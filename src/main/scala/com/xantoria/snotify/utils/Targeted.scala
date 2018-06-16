package com.xantoria.snotify.utils

import com.xantoria.snotify.model.Notification

/**
 * Provides filtering based on notification targets
 *
 * TODO: This might need to be worked into TargetResolution somehow
 */
trait Targeted {
  // Represents which targets should be sent to this destination; the target's ID and its groups
  protected val targets: Set[String]

  /**
   * Filter out notifications which aren't intended for this target
   */
  protected def filterByTarget(n: Notification): Boolean = {
    n.targets.toSet.intersect(targets).nonEmpty
  }
}
