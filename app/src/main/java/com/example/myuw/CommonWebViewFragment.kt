package com.example.myuw

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.*

private var webViewMap: MutableMap<String, WebView> = mutableMapOf()

class CommonWebViewFragment: Fragment() {
    val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    inner class CustomWebViewClient: WebViewClient() {
        lateinit var currentSwipeRefreshLayout: SwipeRefreshLayout

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            currentSwipeRefreshLayout.isRefreshing = false

            // TODO: Remove this when backed styling is done
            webView.evaluateJavascript("document.querySelector(\"body > div:nth-child(4)\").style.display=\"none\"", null)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request!!.url.toString().contains("my-test.s.uw.edu/out?u") || !request.url.toString().contains("my-test")) {
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }
            val bundle = Bundle()
            Log.d("shouldOverrideUrlLoading", "Url: ${request.url}")
            bundle.putCharSequence("base_url", request.url.toString())
            bundle.putCharSequence("title", "test")
            findNavController().navigate(R.id.nav_url_open, bundle)
            return true
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

        if (!webViewMap.containsKey(args.title)) {
            webViewMap[args.title] = WebView(view.context)
            webViewMap[args.title]!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            webViewMap[args.title]!!.settings.javaScriptEnabled = true
            webViewMap[args.title]!!.webViewClient = CustomWebViewClient()
        }

        webView = webViewMap[args.title]!!
        swipeRefreshLayout = view.findViewById(R.id.swipe_to_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }
        webView.setOnScrollChangeListener { _, _, top, _, _ ->
            swipeRefreshLayout.isEnabled = top == 0
        }
        (webView.webViewClient as CustomWebViewClient).currentSwipeRefreshLayout = swipeRefreshLayout


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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // TODO: Use this to maintain the webView's reference for each fragment
    }
}