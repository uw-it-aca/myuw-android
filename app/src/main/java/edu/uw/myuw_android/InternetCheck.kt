package edu.uw.myuw_android

import android.os.AsyncTask
import java.io.IOException
import java.net.InetAddress

class InternetCheck(private val onInternetChecked: (Boolean) -> Unit) :
    AsyncTask<Void, Void, Boolean>() {
    init {
        execute()
    }

    @Deprecated("doInBackground() is deprecated - ??")
    override fun doInBackground(vararg voids: Void): Boolean {
        return try {
            val ipAddr: InetAddress = InetAddress.getByName("www.washington.edu")
            !ipAddr.equals("")
        } catch (e: IOException) {
            false
        }

    }

    @Deprecated("onPostExecute() is deprecated - ??")
    override fun onPostExecute(internet: Boolean) {
        onInternetChecked(internet)
    }
}