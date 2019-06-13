package com.example

import java.net.URLDecoder

object DecodeURL {
  def apply(encodedUrl: String): (String, Boolean) = {
    val splitUrl = encodedUrl.split("urlField=")
    if (splitUrl.length < 2) {
      return ("", false)
    }
    val percentEncoding = splitUrl(1).replace("+", "%20")
    (URLDecoder.decode(percentEncoding, "UTF-8"), true)
  }
}