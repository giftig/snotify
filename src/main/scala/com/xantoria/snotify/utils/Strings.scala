package com.xantoria.snotify.utils

object Strings {
  /**
   * Truncate the given String to the given length by applying an ellipsis or similar character
   */
  def truncate(s: String, limit: Int, char: Char = '…'): String = {
    if (s.length <= limit) s else s.take(limit - 1) + char
  }
}
