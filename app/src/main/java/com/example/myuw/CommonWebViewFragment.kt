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
        // TODO: clicking the same entry multiple times causes the app to crash
        val title = args.title

        Log.d("CommonWebViewFragment", "Title: $title")

        if (!viewFragmentMap.containsKey(title)) {
            viewFragmentMap[title] = inflater.inflate(R.layout.webview_fragment, container, false)
            webView = viewFragmentMap[title]!!.findViewById(R.id.webview_in_fragment)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = CustomWebViewClient()

            webView.setOnScrollChangeListener { _, _, top, _, _ ->
                swipeRefreshLayout.isEnabled = top == 0
            }

            swipeRefreshLayout = viewFragmentMap[title]!!.findViewById(R.id.swipe_to_refresh)
            swipeRefreshLayout.setOnRefreshListener {
                webView.reload()
            }
        }
        webView = viewFragmentMap[title]!!.findViewById(R.id.webview_in_fragment)

        return viewFragmentMap[title]
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