package com.example

import java.net.URLDecoder

object DecodeURL {
  def apply(encodedUrl: String): (String, Boolean) = {
    val parts = encodedUrl.split("urlField=")
    if (parts.length < 2) {
      return ("", false)
    }
    val percentEncoding = parts(1).replace("+", "%20")
    (URLDecoder.decode(percentEncoding, "UTF-8"), true)
  }
}