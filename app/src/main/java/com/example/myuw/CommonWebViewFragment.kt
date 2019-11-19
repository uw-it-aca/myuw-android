package com.example.myuw

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

private var viewFragmentMap: MutableMap<String, View> = mutableMapOf()

class CommonWebViewFragment: Fragment() {
    val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    inner class CustomWebViewClient: WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val name = args.baseUrl.split('/')[3]
        Log.d("CommonWebViewFragment", "Name: $name")

        if (!viewFragmentMap.containsKey(name)) {
            viewFragmentMap[name] = inflater.inflate(R.layout.webview_fragment, container, false)
            webView = viewFragmentMap[name]!!.findViewById(R.id.webview_in_fragment)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = CustomWebViewClient()

            webView.setOnScrollChangeListener { _, _, top, _, _ ->
                swipeRefreshLayout.isEnabled = top == 0
            }

            swipeRefreshLayout = viewFragmentMap[name]!!.findViewById(R.id.swipe_to_refresh)
            swipeRefreshLayout.setOnRefreshListener {
                webView.reload()
            }
        }
        webView = viewFragmentMap[name]!!.findViewById(R.id.webview_in_fragment)

        return viewFragmentMap[name]
    }

    override fun onStart() {
        super.onStart()
        if (webView.url != args.baseUrl)
            webView.loadUrl(args.baseUrl)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()
    }
}