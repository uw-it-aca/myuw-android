package edu.uw.myuw_android

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import edu.my.myuw_android.R
import net.openid.appauth.*

class LoginActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()

        AuthorizationServiceConfiguration.fetchFromUrl(
            Uri.parse(resources.getString(R.string.openid_discovery_uri))
        ) { serviceConfiguration, ex ->
            if (serviceConfiguration != null) {
                val authRequestBuilder = AuthorizationRequest.Builder(
                    serviceConfiguration,
                    resources.getString(R.string.openid_client_id),
                    ResponseTypeValues.CODE,
                    Uri.parse(resources.getString(R.string.openid_redirect_uri))
                )

                val authRequest = authRequestBuilder
                    .setScope(resources.getString(R.string.openid_authorization_scope))
                    .build()

                AuthorizationService(this).performAuthorizationRequest(
                    authRequest,
                    PendingIntent.getActivity(this, 0, Intent(this, NavDrawerMain::class.java), 0),
                    PendingIntent.getActivity(this, 0, Intent(this, this::class.java), 0)
                )
            } else {
                Log.e("AuthorizationServiceConfiguration", ex!!.localizedMessage)
            }
        }
    }
}