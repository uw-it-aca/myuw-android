package edu.uw.myuw_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.*
import org.json.JSONObject

class LoginActivity: AppCompatActivity() {

    val RC_AUTH = 132
    lateinit var authService: AppAuthWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()

        loginButton.setOnClickListener {
            tryLoginWithAppAuth()
        }

        authService = AppAuthWrapper(this)

        if (authService.couldBeAuthorized) {
            signed_status.text = getString(R.string.signed_in)
            loginButton.isClickable = false
            beforeLogin.visibility = ViewGroup.GONE
            afterLogin.visibility = ViewGroup.VISIBLE
            startMainActivity()
        } else {
            signed_status.text = getString(R.string.not_signed_in)
        }
    }

    override fun onStop() {
        super.onStop()
        authService.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_AUTH) {
            data?.also { it ->
                authService.updateAuthWithIntent(it) { ex ->
                    if (ex == null) {
                        startMainActivity()
                    } else {
                        ex.localizedMessage?.also { localizedMessage ->
                            Log.e("AuthorizationResponse", localizedMessage)
                        }
                        authService.showAuthenticationError()
                    }
                }
            } ?: authService.showAuthenticationError()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tryLoginWithAppAuth() {
        loginButton.isEnabled = false
        InternetCheck {
            if (it) {
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

                        beforeLogin.visibility = ViewGroup.GONE
                        afterLogin.visibility = ViewGroup.VISIBLE
                        startActivityForResult(
                            authService.getAuthorizationRequestIntent(authRequest),
                            RC_AUTH
                        )
                    } else {
                        ex?.localizedMessage?.also {
                            Log.e("AuthorizationServiceConfiguration", ex.toString())
                        }
                        authService.showAuthenticationError()
                    }
                }
            } else raiseNoInternet()
        }
    }

    private fun startMainActivity() {
        InternetCheck {
            if (it) {
                val job = GlobalScope.launch {
                    UserInfoStore.updateAffiliations(this@LoginActivity, resources, authService)
                }

                val idObject = JSONObject(String(Base64.decode(authService.authState!!.idToken!!.split(".")[1], Base64.URL_SAFE)))
                UserInfoStore.name.postValue(idObject["sub"] as String)
                // TODO: Fix this
                // UserInfoStore.email.postValue(idObject["email"] as String)
                // UserInfoStore.netId.postValue((idObject["email"] as String).split('@')[0])

                runBlocking {
                    job.join()
                }

                Log.d("LoginActivity: startMainActivity", "accessToken: ${authService.authState!!.accessToken}")
                Log.d("LoginActivity: startMainActivity", "idToken: ${authService.authState!!.idToken}")
                Log.d("LoginActivity: startMainActivity", "idObject: $idObject")

                val intent = Intent(this, NavDrawerMain::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else raiseNoInternet()
        }
    }

    private fun raiseNoInternet() {
        ErrorActivity.showError(
            resources.getString(R.string.no_internet),
            resources.getString(R.string.no_internet_desc),
            resources.getString(R.string.onReceiveErrorButton),
            ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
            this
        )
    }
}