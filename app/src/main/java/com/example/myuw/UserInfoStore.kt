package com.example.myuw

import android.webkit.JavascriptInterface

object UserInfoStore {
    var netId: String = ""
    var emailForwardUrl: String = ""
    var groups: Set<String> = HashSet<String>()

    @JavascriptInterface
    fun decodeUserStream(token: String, tokenValue: String) {
        when {
            tokenValue == "true" -> groups.plus(token)
            token == "netid" -> netId = tokenValue
            token == "email_forward_url" -> emailForwardUrl = tokenValue
        }
    }
}