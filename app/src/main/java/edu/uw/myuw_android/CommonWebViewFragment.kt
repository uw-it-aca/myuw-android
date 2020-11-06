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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.webview_fragment.view.*
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class CommonWebViewFragment: Fragment() {
    public val args: CommonWebViewFragmentArgs by navArgs()
    lateinit var webView: WebView
    lateinit var baseUrl: String
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var authState: AuthStateWrapper

    companion object {
        var webViewMap: MutableMap<String, WebView> = mutableMapOf()
    }

    inner class CustomWebViewClient: WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false
            swipeRefreshLayout.beforeWebViewLoad.visibility = View.GONE
            view.visibility = View.VISIBLE

            if (url.endsWith("/logout")) {
                Log.d("onPageFinished", "Logging out")
                authState.deleteAuth()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.putExtra("LOGGED_OUT", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                activity?.finish()
            } else {
                // A null here can be safely ignored because the this means the fragment was detached before page load was finished
                (activity as? AppCompatActivity)?.supportActionBar?.let {
                    val title = view.title.split(": ").getOrElse(1) { "" }
                    it.title = if (title == "Home") "MyUW" else title
                }
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            Log.d("shouldOverrideUrlLoading", "before processing url: ${request.url}")
            if (request.url.toString().contains("$baseUrl/out?u=") || !request.url.toString().contains(URL(baseUrl).host)) {
                val decodedUrl = URLDecoder.decode(
                    request.url.toString().replace(Regex("^.+\\?u=|&l.+$"), ""),
                    StandardCharsets.UTF_8.toString()
                )
                Log.d("shouldOverrideUrlLoading", "decodedUrl: $decodedUrl")
                val uri = Uri.parse(decodedUrl)

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
            Log.d(" - onReceivedHttpError", errorResponse?.reasonPhrase.toString());
            if (errorResponse?.statusCode == 401) {
                if (view?.url?.endsWith("/logout") == true) {
                    authState.deleteAuth()
                } else {
                    InternetCheck {
                        if (it) {
                            authState.performActionWithFreshTokens({ accessToken, idToken ->
                                Log.d("AppAuth", "got 401")
                                Log.d("AppAuth", "accessToken: $accessToken")
                                Log.d("AppAuth", "idToken: $idToken")

                                webView.loadUrl(baseUrl + args.path,
                                    hashMapOf("Authorization" to "Bearer ${authState.idToken}"))
                            }, { ex ->
                                ex?.localizedMessage?.also {
                                    Log.e("AuthorizationServiceConfiguration", ex.toString())
                                }
                                authState.showAuthenticationError()
                            }, true)
                        } else raiseNoInternet()
                    }
                }
            } else if (errorResponse?.statusCode == 500) {
                activity?.let {
                    ErrorActivity.showError(
                        "Unable to load page",
                        "A server error has occurred. We are aware of the issue and are working to resolve it. Please try again in a few minutes.",
                        "Retry",
                        ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                        it
                    )
                }
            } else super.onReceivedHttpError(view, request, errorResponse)
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
                it.visibility = View.INVISIBLE
            }
        }
        else view.beforeWebViewLoad.visibility = View.GONE

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
            webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                swipeRefreshLayout.isEnabled = scrollY == 0
            }

            swipeRefreshLayout.isEnabled = webView.scrollY == 0
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
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.title = ""
        }
        // A null here can be safely ignored because the this means the fragment was detached
        activity?.let {
            authState = AuthStateWrapper(it)
            if (webView.url == null) {
                Log.d("onStart", "Loading url: ${baseUrl + args.path}")
                Log.d("onStart", "With IdToken: ${authState.idToken}")
                webView.loadUrl(
                    baseUrl + args.path,
                    hashMapOf("Authorization" to "Bearer ${authState.idToken}")
                )
            }
        }
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()

        (activity as? AppCompatActivity)?.supportActionBar?.let {
            val title = webView.title.split(": ").getOrElse(1) { "" }
            it.title = if (title == "Home") "MyUW" else title
        }

    }

    private fun raiseNoInternet() {
        activity?.let {
            ErrorActivity.showError(
                resources.getString(R.string.no_internet),
                resources.getString(R.string.no_internet_desc),
                resources.getString(R.string.onReceiveErrorButton),
                ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                it
            )
        }
    }

    private fun raiseUnableToConnect() {
        activity?.let {
            ErrorActivity.showError(
                resources.getString(R.string.onReceiveError),
                resources.getString(R.string.onReceiveErrorDesc),
                resources.getString(R.string.onReceiveErrorButton),
                ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                it
            )
        }
    }
}
