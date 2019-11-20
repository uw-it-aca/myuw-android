package com.example.myuw

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class LoginActivity: AppCompatActivity() {
    lateinit var webView: WebView

    inner class CustomWebViewClient: WebViewClient() {
        /*override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d("LoginActivity - CustomWebViewClient", url)
            if (url == "https://my-test.s.uw.edu/") {
                startMainActivity()
            }
        }*/

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url == "https://my-test.s.uw.edu/") {
                webView.evaluateJavascript("for (x in user) if (user[x]) NativeLogin.decodeUserStream(x, user[x])", null)
                startMainActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        webView = findViewById(R.id.webview_in_login)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = CustomWebViewClient()
        webView.addJavascriptInterface(UserInfoStore, "NativeLogin")

        webView.loadUrl("https://my-test.s.uw.edu/saml/login?next=/")
    }

    fun startMainActivity() {
        val intent = Intent(this, NavDrawerMain::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(intent)
        this.finish()
    }
}