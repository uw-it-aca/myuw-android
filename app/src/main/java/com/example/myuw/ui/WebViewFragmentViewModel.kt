package com.example.myuw.ui

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WebViewFragmentViewModel : ViewModel() {

    private val _rootView = MutableLiveData<View?>().apply {
        value = null
    }
    val rootView: LiveData<View?> = _rootView

    fun updateRootView(value: View) {
        _rootView.value = value
    }
    //var rootView: View? = null
}