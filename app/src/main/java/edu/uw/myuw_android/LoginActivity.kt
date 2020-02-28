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
    lateinit var authorizationService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginButton.setOnClickListener {
            tryLoginWithAppAuth()
        }

        authorizationService = AuthorizationService(this)

        if (UserInfoStore.readAuthState(this).isAuthorized) {
            signed_status.text = getString(R.string.signed_in)
            loginButton.isClickable = false
            startMainActivity()
        } else {
            signed_status.text = getString(R.string.not_signed_in)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authorizationService.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_AUTH) {
            data?.also {
                val resp = AuthorizationResponse.fromIntent(it)
                val ex = AuthorizationException.fromIntent(it)

                if (resp != null) {
                    val authState = AuthState(resp, ex)
                    authorizationService.performTokenRequest(
                        resp.createTokenExchangeRequest()
                    ) { response, ex_new ->
                        if (response != null) {
                            authState.update(response, ex)
                            UserInfoStore.writeAuthState(this, authState)
                            startMainActivity()
                        } else {
                            ex_new?.localizedMessage?.also { localizedMessage ->
                                Log.e("performTokenRequest", localizedMessage)
                            }
                            showAuthenticationError()
                        }
                    }
                } else {
                    ex?.localizedMessage?.also { localizedMessage ->
                        Log.e("AuthorizationResponse", localizedMessage)
                    }
                    showAuthenticationError()
                }
            } ?: showAuthenticationError()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tryLoginWithAppAuth() {
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
                    authorizationService.getAuthorizationRequestIntent(authRequest),
                    RC_AUTH
                )
            } else {
                ex?.localizedMessage?.also {
                    Log.e("AuthorizationServiceConfiguration", it)
                }
                showAuthenticationError()
            }
        }
    }

    private fun startMainActivity() {
        val authState = UserInfoStore.readAuthState(this)
        authState.performActionWithFreshTokens(authorizationService) {
                accessToken, idToken, _ ->

            idToken?.also {
                val job = GlobalScope.launch {
                    UserInfoStore.updateAffiliations(this@LoginActivity, resources, it)
                }

                val idObject = JSONObject(String(Base64.decode(it.split(".")[1], Base64.URL_SAFE)))
                UserInfoStore.name.postValue(idObject["sub"] as String)
                UserInfoStore.email.postValue(idObject["email"] as String)
                UserInfoStore.netId.postValue((idObject["email"] as String).split('@')[0])

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
            } ?: showAuthenticationError()
        }
    }

    private fun showAuthenticationError() {
        ErrorActivity.showError(
            "Unable to Sign In",
            "There was an error while trying to get auth tokens. This message needs to be updated by ux",
            "Retry",
            ErrorActivity.ErrorHandlerEnum.RETRY_LOGIN,
            this
        )
    }
}