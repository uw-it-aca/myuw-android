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
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.my.myuw_android.BuildConfig
import edu.my.myuw_android.R
import net.openid.appauth.AuthorizationService
import java.lang.Exception
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

private var webViewMap: MutableMap<String, WebView> = mutableMapOf()

class CommonWebViewFragment: Fragment() {
    private val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var baseUrl: String
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var authorizationService: AuthorizationService

    inner class CustomWebViewClient: WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false

            (activity as? AppCompatActivity)?.supportActionBar!!.title = webView.title.split(": ").getOrElse(1){"Invalid Title"}
            // TODO: Remove this when backed styling is done
            webView.evaluateJavascript("document.querySelector(\"body > div:nth-child(4)\").style.display=\"none\"", null)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request!!.url.toString().contains("$baseUrl/out?u=") || !request.url.toString().contains(URL(baseUrl).host)) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString().replace("$baseUrl/out?u=", ""))))
            } else {
                val bundle = Bundle()
                Log.d("shouldOverrideUrlLoading", "Url: ${request.url}")
                bundle.putCharSequence("base_url", request.url.toString())
                bundle.putCharSequence("title", "")
                bundle.putCharSequence("unique_id", UUID.randomUUID().toString())
                findNavController().navigate(R.id.nav_url_open, bundle)
            }
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            if (request!!.url!!.scheme == "http" && !BuildConfig.DEBUG) {
                try {
                    val httpsUrl = URL(request.url.toString().replace("http://", "https://"))
                    val connection = httpsUrl.openConnection()
                    return WebResourceResponse(connection.contentType, connection.contentEncoding, connection.getInputStream())
                } catch (e: Exception) {
                    throw e
                }
            }
            return super.shouldInterceptRequest(view, request)
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
        Log.d("CommonWebViewFragment", "unique_id: ${args.uniqueId}")

        baseUrl = resources.getString(R.string.myuw_base_url)

        if (!webViewMap.containsKey(args.uniqueId)) {
            webViewMap[args.uniqueId] = WebView(view.context)
            webViewMap[args.uniqueId]!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            webViewMap[args.uniqueId]!!.settings.javaScriptEnabled = true
        }

        webView = webViewMap[args.uniqueId]!!
        webView.webViewClient = CustomWebViewClient()
        webView.settings.userAgentString += " MyUW_Hybrid/1.0 (Android)"
        Log.d("UserAgentString", webView.settings.userAgentString)
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
            ) { accessToken, idToken, _ ->
                Log.d("AppAuth", "accessToken: $accessToken")
                Log.d("AppAuth", "idToken: $idToken")
                val baseURL: String = if (args.baseUrl.startsWith("@")) {
                    resources.getString(resources.getIdentifier(args.baseUrl.substring(1), "string", context!!.packageName))
                } else {
                    args.baseUrl
                }
                webView.loadUrl(baseURL, hashMapOf("Authorization" to "Bearer $idToken"))
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