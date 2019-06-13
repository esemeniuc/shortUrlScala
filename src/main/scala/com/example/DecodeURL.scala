package com.example

import java.net.URLDecoder
import AkkaQuickstart.rootUrl

object DecodeURL {
  def apply(encodedUrl: String): (String, Boolean) = {
    val parts = encodedUrl.split("urlField=")
    if (parts.length < 2) {
      return ("", false)
    }
    val percentEncoding = parts(1).replace("+", "%20")
    (URLDecoder.decode(percentEncoding, "UTF-8"), true)
  }

  def getUrlShortcode(url: String): String = {
    val parts = url.split(rootUrl)

    return ""
  }
}