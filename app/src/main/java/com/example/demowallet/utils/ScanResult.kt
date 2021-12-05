package com.example.demowallet.utils

import android.net.Uri
import java.net.URL

data class ScanResult(val string : String) {
    val uri : Uri get() { return Uri.parse(string) }
    val url : URL get() { return URL("https://" + uri.host) }
    val userId : String get() { return uri.getQueryParameter("userId")!! }
    val secret : String get() { return uri.getQueryParameter("secret")!! }
    val sessionId : String get() { return uri.getQueryParameter("sessionId")!! }
}