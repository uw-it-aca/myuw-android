package com.example.myuw.ui.page1

import android.util.Log
import androidx.navigation.fragment.navArgs
import com.example.myuw.ui.WebviewFragment

class page1Fragment : WebviewFragment {

    val args: page1FragmentArgs by navArgs()

    constructor() : super() {
        Log.d("page1Fragment", "Page 1 constructed")
    }

    override fun onStart() {
        super.onStart()
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