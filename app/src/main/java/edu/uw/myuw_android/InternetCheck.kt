package edu.uw.myuw_android

import android.os.AsyncTask
import java.io.IOException
import java.net.InetAddress

class InternetCheck(private val onInternetChecked: (Boolean) -> Unit) :
    AsyncTask<Void, Void, Boolean>() {
    init {
        execute()
    }

    override fun doInBackground(vararg voids: Void): Boolean {
        return try {
            val ipAddr: InetAddress = InetAddress.getByName("google.com")
            !ipAddr.equals("")
        } catch (e: IOException) {
            false
        }

    }

    override fun onPostExecute(internet: Boolean) {
        onInternetChecked(internet)
    }
}