package com.example.myuw.ui

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

abstract class WebviewFragment : Fragment() {
    protected lateinit var webView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val vm = ViewModelProviders.of(this).get(WebViewFragmentViewModel::class.java)
        if (vm.rootView.value == null) {
            Log.d("WebviewFragment", "New fragment was created here")
            vm.updateRootView(inflater.inflate(R.layout.webview_fragment, container, false))
        }
        webView = vm.rootView.value!!.findViewById(R.id.webview_in_fragment)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        return vm.rootView.value
    }
}