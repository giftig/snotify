package com.xantoria.snotify.rest

object Utils {
  /**
   * Represents a basic acknowledging response from an API endpoint
   *
   * Used when a simple message is required, rather than some other object being returned
   */
  case class BasicResponse(
    message: String = "ok",
    success: Boolean = true,
    reason: Option[String] = None
  )

  object BasicResponse {
    def apply(t: Throwable): BasicResponse = BasicResponse(
      message = t.getMessage,
      success = false,
      reason = Some(t.getClass.getName)
    )
  }
}
