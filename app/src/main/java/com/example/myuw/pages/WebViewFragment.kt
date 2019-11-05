package com.example.myuw.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.myuw.R

abstract class WebViewFragment : Fragment() {
    protected lateinit var webView: WebView

    protected fun getView(inflater: LayoutInflater, container: ViewGroup?, rootView: View?): View {
        val newRootView =
            rootView ?: inflater.inflate(R.layout.webview_fragment, container, false)

        webView = newRootView.findViewById(R.id.webview_in_fragment)
        if (rootView == null) {
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = WebViewClient()
        }

        return newRootView
    }
}