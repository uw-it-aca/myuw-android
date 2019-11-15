package com.example.myuw

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment

class ProfileWebViewDialog : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // val inflatedView = inflater.inflate(R.layout.fragment_home, container, false)

        val inflatedView = inflater.inflate(R.layout.webview_fragment, container, false)
        // inflatedView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val webView = inflatedView.findViewById<WebView>(R.id.webview_in_fragment)
        webView.layoutParams = LinearLayout.LayoutParams(1000, 1400)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://my-test.s.uw.edu/profile/")

        return inflatedView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}