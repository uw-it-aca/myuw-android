package edu.uw.myuw_android

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.my.myuw_android.BuildConfig
import edu.my.myuw_android.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class LoginActivity: AppCompatActivity() {

    val RC_AUTH = 132
    lateinit var authorizationService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        findViewById<Button>(R.id.button).setOnClickListener {
            tryLoginWithAppAuth()
        }

        authorizationService = AuthorizationService(this)

        if (UserInfoStore.readAuthState(this).isAuthorized) {
            findViewById<TextView>(R.id.textView).text = "You are Authorized"
            findViewById<Button>(R.id.button).isClickable = false
            startMainActivity()
        } else {
            findViewById<TextView>(R.id.textView).text = "You are not Authorized"
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
                            TODO("Show error page if this point is reached")
                        }
                    }
                } else {
                    ex?.localizedMessage?.also { localizedMessage ->
                        Log.e("AuthorizationResponse", localizedMessage)
                    }
                    TODO("Show error page if this point is reached")
                }
            } ?: TODO("Error page for no response/improper from OAuth provider")
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
                TODO("Show error page if this point is reached")
            }
        }
    }

    private fun startMainActivity() {
        val authState = UserInfoStore.readAuthState(this)
        authState.performActionWithFreshTokens(authorizationService) {
                accessToken, idToken, _ ->

            idToken?.also {
                val job = GlobalScope.launch {
                    UserInfoStore.updateAffiliations(resources, it)
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
            } ?: TODO("Show the error page here with some info about the id token not existing")
        }
    }
}