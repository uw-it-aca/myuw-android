package edu.uw.myuw_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
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

        loginButton.setOnClickListener {
            tryLoginWithAppAuth()
        }

        authService = AppAuthWrapper(this)

        if (authService.isAuthorized) {
            signed_status.text = getString(R.string.signed_in)
            loginButton.isClickable = false
            startMainActivity()
        } else {
            signed_status.text = getString(R.string.not_signed_in)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
                        showAuthenticationError()
                    }
                }
            } ?: showAuthenticationError()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tryLoginWithAppAuth() {
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

                        startActivityForResult(
                            authService.getAuthorizationRequestIntent(authRequest),
                            RC_AUTH
                        )
                    } else {
                        ex?.localizedMessage?.also {
                            Log.e("AuthorizationServiceConfiguration", ex.toString())
                        }
                        showAuthenticationError()
                    }
                }
            } else raiseNoInternet()
        }
    }

    private fun startMainActivity() {
        InternetCheck {
            if (it) {
                authService.performActionWithFreshTokens({ accessToken, idToken ->
                    val job = GlobalScope.launch {
                        UserInfoStore.updateAffiliations(this@LoginActivity, resources, idToken)
                    }

                    val idObject = JSONObject(String(Base64.decode(idToken.split(".")[1], Base64.URL_SAFE)))
                    UserInfoStore.name.postValue(idObject["sub"] as String)
                    // TODO: Fix this
                    // UserInfoStore.email.postValue(idObject["email"] as String)
                    // UserInfoStore.netId.postValue((idObject["email"] as String).split('@')[0])

                    runBlocking {
                        job.join()
                    }

                    Log.d("LoginActivity: startMainActivity", "accessToken: $accessToken")
                    Log.d("LoginActivity: startMainActivity", "idToken: $idToken")
                    Log.d("LoginActivity: startMainActivity", "idObject: $idObject")

                    val intent = Intent(this, NavDrawerMain::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }, { ex ->
                    ex?.localizedMessage?.also {
                        Log.e("AuthorizationServiceConfiguration", ex.toString())
                    }
                    showAuthenticationError()
                })
            } else raiseNoInternet()
        }
    }

    private fun showAuthenticationError() {
        ErrorActivity.showError(
            resources.getString(R.string.sign_in_error),
            resources.getString(R.string.sign_in_error_desc),
            resources.getString(R.string.onReceiveErrorButton),
            ErrorActivity.ErrorHandlerEnum.RETRY_LOGIN,
            this
        )
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