package edu.uw.myuw_android

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_search.*
import java.io.InputStream


class SearchActivity : AppCompatActivity() {

    private lateinit var query:String

    inner class CustomWebViewClient: WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            injectCSS(view!!, "injected_search.css")
            super.onPageFinished(view, url)
        }

        private fun injectCSS(webView: WebView, fileName: String) {
            try {
                val inputStream: InputStream = assets.open(fileName)
                val buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                inputStream.close()
                val encoded: String = Base64.encodeToString(buffer, Base64.NO_WRAP)
                webView.loadUrl(
                    "javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var style = document.createElement('style');" +
                            "style.type = 'text/css';" +  // Tell the browser to BASE64-decode the string into your script !!!
                            "style.innerHTML = window.atob('" + encoded + "');" +
                            "parent.appendChild(style)" +
                            "})()"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onStart() {
        title = "Search the UW"
        search_webview.settings.javaScriptEnabled = true
        search_webview.webViewClient = CustomWebViewClient()

        search_webview.loadUrl("https://www.washington.edu/search/?q=$query")
        super.onStart()
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                doSearch(query)
            }
        }
    }

    private fun doSearch(query: String) {
        this.query = query
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}
