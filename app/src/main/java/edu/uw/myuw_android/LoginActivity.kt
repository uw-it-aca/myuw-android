package edu.uw.myuw_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.BulletSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.json.JSONObject

class LoginActivity: AppCompatActivity() {

    val RC_AUTH = 132
    lateinit var authState: AuthStateWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()

        loginButton.setOnClickListener {
            tryLoginWithAppAuth()
        }

        AuthStateWrapper.tryAuthServiceInit(this)
        authState = AuthStateWrapper(this)

        if (authState.couldBeAuthorized) {
            login_status.text = getString(R.string.signed_in)
            loginButton.isClickable = false

            beforeLogin.visibility = ViewGroup.GONE
            afterLogin.visibility = ViewGroup.VISIBLE
            startMainActivity()
        } else {
            if (intent.getBooleanExtra("LOGGED_OUT", false)) {
                login_status.text = getString(R.string.signed_out)
                login_desc.text = getString(R.string.signed_out_desc)
                login_list.visibility = ViewGroup.GONE
            } else {
                login_status.text = getString(R.string.not_signed_in)
                login_desc.text = getString(R.string.login_info)
                login_list.visibility = ViewGroup.VISIBLE
                login_list.text = TextUtils.concat(
                    getBulletedList(getText(R.string.login_list_2)),
                    getBulletedList(getText(R.string.login_list_3)),
                    getBulletedList(getText(R.string.login_list_4))
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AuthStateWrapper.tryAuthServiceInit(this)
    }

    override fun onPause() {
        super.onPause()
        AuthStateWrapper.tryAuthServiceDispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_AUTH) {
            data?.also { it ->
                authState.updateAuthWithIntent(it) { ex ->
                    if (ex == null) {
                        startMainActivity()
                    } else {
                        ex.localizedMessage?.also { localizedMessage ->
                            Log.e("AuthorizationResponse", localizedMessage)
                        }
                        authState.showAuthenticationError()
                    }
                }
            } ?: authState.showAuthenticationError()
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
                            .setPrompt("login")
                            .build()

                        beforeLogin.visibility = ViewGroup.GONE
                        afterLogin.visibility = ViewGroup.VISIBLE
                        startActivityForResult(
                            authState.getAuthorizationRequestIntent(authRequest),
                            RC_AUTH
                        )
                    } else {
                        ex?.localizedMessage?.also {
                            Log.e("AuthorizationServiceConfiguration", ex.toString())
                        }
                        authState.showAuthenticationError()
                    }
                }
            } else raiseNoInternet()
        }
    }

    private fun startMainActivity() {
        InternetCheck {
            if (it) {
                val job = GlobalScope.launch {
                    UserInfoStore.updateAffiliations(this@LoginActivity, resources, authState)
                }

                val idObject = JSONObject(String(Base64.decode(authState.idToken!!.split(".")[1], Base64.URL_SAFE)))
                UserInfoStore.name.postValue(idObject["sub"] as String)

                runBlocking {
                    job.join()
                }

                Log.d("LoginActivity: startMainActivity", "accessToken: ${authState.accessToken}")
                Log.d("LoginActivity: startMainActivity", "idToken: ${authState.idToken}")
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

    fun openEULA(_v: View) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.myuw_eula_url))
        )
        startActivity(browserIntent)
    }

    fun openPrivacy(_v: View) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.myuw_privacy_url))
        )
        startActivity(browserIntent)
    }

    fun openTOS(_v: View) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.myuw_tos_url))
        )
        startActivity(browserIntent)
    }

    fun openHelp(_v: View) {
        val browserIntent = Intent(
            Intent.ACTION_SENDTO, Uri.parse("mailto:" + resources.getString(R.string.help_uw_edu))
        )
        startActivity(browserIntent)
    }

    private fun getBulletedList(cs: CharSequence): SpannableString {
        val s = SpannableString(cs)
        s.setSpan(BulletSpan(15), 0, cs.length, 0)
        return s
    }
}