package com.example.myuw.pages.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.myuw.pages.WebViewFragment

@SuppressLint("StaticFieldLeak")
private var rootView: View? = null

class ProfileWebViewFragment : WebViewFragment() {
    val args: ProfileWebViewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = getView(inflater, container, rootView)
        return rootView
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