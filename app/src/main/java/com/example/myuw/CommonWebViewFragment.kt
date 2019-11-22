package com.example.myuw

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

private var viewFragmentMap: MutableMap<String, WebView> = mutableMapOf()

class CommonWebViewFragment: Fragment() {
    val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    inner class CustomWebViewClient: WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false

            // TODO: Remove this when backed styling is done
            webView.evaluateJavascript("document.querySelector(\"body > div:nth-child(4)\").style.display=\"none\"", null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.webview_fragment, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("CommonWebViewFragment", "Title: ${args.title}")

        if (!viewFragmentMap.containsKey(args.title)) {
            viewFragmentMap[args.title] = WebView(view.context)
            viewFragmentMap[args.title]!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            viewFragmentMap[args.title]!!.settings.javaScriptEnabled = true
            viewFragmentMap[args.title]!!.webViewClient = CustomWebViewClient()

            viewFragmentMap[args.title]!!.setOnScrollChangeListener { _, _, top, _, _ ->
                swipeRefreshLayout.isEnabled = top == 0
            }
        }

        webView = viewFragmentMap[args.title]!!
        swipeRefreshLayout = view.findViewById(R.id.swipe_to_refresh)
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        (webView.parent as ViewGroup?)?.removeView(webView)
        view.findViewById<LinearLayout>(R.id.webView_attach_point).addView(webView)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        swipeRefreshLayout.findViewById<LinearLayout>(R.id.webView_attach_point).removeView(webView)
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