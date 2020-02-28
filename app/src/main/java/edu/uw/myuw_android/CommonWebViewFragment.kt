package edu.uw.myuw_android

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.my.myuw_android.BuildConfig
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.webview_fragment.view.*
import net.openid.appauth.AuthorizationService
import java.lang.Exception
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false

            // A null here can be safely ignored because the this means the fragment was detached before page load was finished
            (activity as? AppCompatActivity)?.supportActionBar?.let {
                it.title = view.title.split(": ").getOrElse(1){"Invalid Title"}
            } ?: TODO("Gracefully crash the app? webview probably finished loading after the fragment was unloaded. Could just ignore this")
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            Log.d("shouldOverrideUrlLoading", "before processing url: ${request.url}")
            if (request.url.toString().contains("$baseUrl/out?u=") || !request.url.toString().contains(URL(baseUrl).host)) {
                val decodedUrl = URLDecoder.decode(request.url.toString().replace("$baseUrl/out?u=", ""), StandardCharsets.UTF_8.toString())
                val uri = Uri.parse(decodedUrl).buildUpon().scheme("http").build()

                Log.d("shouldOverrideUrlLoading", "Url: $uri")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } else {
                val bundle = Bundle()
                Log.d("shouldOverrideUrlLoading", "Url: ${request.url}")
                bundle.putCharSequence("path", request.url.toString().replace(baseUrl, ""))
                bundle.putCharSequence("title", "")
                bundle.putCharSequence("unique_id", UUID.randomUUID().toString())
                findNavController().navigate(R.id.nav_url_open, bundle)
            }
            return true
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            Log.e("CustomWebViewClient - onReceivedError", error.toString())
            InternetCheck {
                if (it) raiseUnableToConnect() else raiseNoInternet()
            }
            super.onReceivedError(view, request, error)
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            Log.e("CustomWebViewClient - onReceivedHttpError", errorResponse.toString())
            raiseUnableToConnect()
            super.onReceivedHttpError(view, request, errorResponse)
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
            // No way to gracefully handle this
            webViewMap[args.uniqueId]!!.let {
                it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                it.settings.javaScriptEnabled = true
            }
        }

        // No way to gracefully handle this
        webViewMap[args.uniqueId]!!.also {
            webView = it
            webView.webViewClient = CustomWebViewClient()
            webView.settings.userAgentString += " MyUW_Hybrid/1.0 (Android)"
            Log.d("UserAgentString", webView.settings.userAgentString)
            swipeRefreshLayout = view.swipe_to_refresh

            swipeRefreshLayout.setOnRefreshListener {
                webView.reload()
            }
            webView.setOnScrollChangeListener { _, _, top, _, _ ->
                swipeRefreshLayout.isEnabled = top == 0
            }

            (webView.parent as ViewGroup?)?.removeView(webView)
            view.webView_attach_point.addView(webView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        swipeRefreshLayout.webView_attach_point.removeView(webView)
    }

    override fun onStart() {
        super.onStart()
        // A null here can be safely ignored because the this means the fragment was detached
        (activity as AppCompatActivity).supportActionBar?.let {
            it.title = ""
        }
        // A null here can be safely ignored because the this means the fragment was detached
        activity?.let {
            authorizationService = AuthorizationService(it)
            if (webView.url == null)
                InternetCheck { internet ->
                    if (internet) {
                        UserInfoStore.readAuthState(it).performActionWithFreshTokens(
                            authorizationService
                        ) { accessToken, idToken, _ ->
                            Log.d("AppAuth", "accessToken: $accessToken")
                            Log.d("AppAuth", "idToken: $idToken")
                            Log.d("AppAuth", "url: ${baseUrl + args.path}")
                            webView.loadUrl(
                                baseUrl + args.path,
                                hashMapOf("Authorization" to "Bearer $idToken")
                            )
                        }
                    } else raiseNoInternet()
                }
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
            // A null here can be safely ignored because the this means the fragment was detached
            (activity as AppCompatActivity).supportActionBar?.let {
                it.title = webView.title.split(": ")[1]
            }
        super.onResume()
    }

    private fun raiseNoInternet() {
        activity?.let {
            ErrorActivity.showError(
                "No Internet Connection",
                "Please connect to internet. This message needs to be updated by ux",
                "Retry",
                ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                it
            )
        }
    }

    private fun raiseUnableToConnect() {
        activity?.let {
            ErrorActivity.showError(
                "Unable to Load Page",
                "A server error has occurred. We are aware of this issue and are working on it. Please try again in a few minutes. This message needs to be updated by ux",
                "Retry",
                ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                it
            )
        }
    }
}
