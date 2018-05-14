package com.xantoria.snotify.alert

import scala.concurrent.{ExecutionContext, Future}

import com.xantoria.snotify.model.Notification

/**
 * Handles displaying some sort of user alert for a notification, eg. a message or a sound
 */
trait AlertHandling {
  /**
   * Trigger the alert, which may involve acknowledgement
   *
   * @returns Boolean indicating whether the alert was successfully acknowledged (always true if
   *          no acknowledgement happens for this alert type)
   */
  def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean]
}
