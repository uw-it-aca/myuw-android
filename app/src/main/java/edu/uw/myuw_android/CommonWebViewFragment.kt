package edu.uw.myuw_android

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.my.myuw_android.R
import net.openid.appauth.AuthorizationService
import java.util.*
import kotlin.collections.HashMap

private var webViewMap: MutableMap<String, WebView> = mutableMapOf()

class CommonWebViewFragment: Fragment() {
    val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var authorizationService: AuthorizationService

    inner class CustomWebViewClient: WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false

            (activity as AppCompatActivity).supportActionBar!!.title = webView.title.split(": ")[1]
            // TODO: Remove this when backed styling is done
            webView.evaluateJavascript("document.querySelector(\"body > div:nth-child(4)\").style.display=\"none\"", null)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request!!.url.toString().contains("my-test.s.uw.edu/out?u=") || !request.url.toString().contains("my-test")) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString().replace("http://curry.aca.uw.edu:8000/out?u=", ""))))
            } else {

                val bundle = Bundle()
                Log.d("shouldOverrideUrlLoading", "Url: ${request.url}")
                bundle.putCharSequence("base_url", request.url.toString())
                bundle.putCharSequence("title", UUID.randomUUID().toString())
                findNavController().navigate(R.id.nav_url_open, bundle)
            }
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
        }

        webView = webViewMap[args.title]!!
        webView.webViewClient = CustomWebViewClient()
        // webView.settings.userAgentString = "MyUW_Hybrid/1.0 (Android)"
        // Log.d("UserAgentString", webView.settings.userAgentString)
        swipeRefreshLayout = view.findViewById(R.id.swipe_to_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }
        webView.setOnScrollChangeListener { _, _, top, _, _ ->
            swipeRefreshLayout.isEnabled = top == 0
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
        (activity as AppCompatActivity).supportActionBar!!.title = ""
        authorizationService = AuthorizationService(activity!!)
        if (webView.url == null)
            UserInfoStore.readAuthState(context!!).performActionWithFreshTokens(
                authorizationService
            ) { accessToken, idToken, ex ->
                Log.d("AppAuth", "accessToken: $accessToken")
                Log.d("AppAuth", "idToken: $idToken")
                webView.loadUrl(args.baseUrl, hashMapOf())
            }
    }

    override fun onStop() {
        super.onStop()
        authorizationService.dispose()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        webView.onResume()
        if (webView.title.split(": ").size > 1)
            (activity as AppCompatActivity).supportActionBar!!.title = webView.title.split(": ")[1]
        super.onResume()
    }

    fun onBackPress():Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return false
        }
        return true
    }
}