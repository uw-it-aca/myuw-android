package edu.uw.myuw_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import edu.my.myuw_android.R
import net.openid.appauth.*
import java.util.concurrent.atomic.AtomicReference

class AuthStateWrapper(private val activity: Activity) {
    private val authSharedPreferences: SharedPreferences =
        activity.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val affiliationsSharedPreferences =
        activity.getSharedPreferences("affiliations", Context.MODE_PRIVATE)
    private val resources: Resources = activity.resources

    companion object {
        private var authorizationService: AtomicReference<AuthorizationService> = AtomicReference()

        fun tryAuthServiceInit(activity: Activity): Boolean {
            Log.d("AuthStateWrapperStatic", "AuthStateWrapper - tryAuthServiceInit")
            authorizationService.get()?.let {
                return false
            }
            Log.d("AuthStateWrapperStatic", "AuthStateWrapper - tryAuthServiceInit: create new")
            authorizationService.set(AuthorizationService(activity))
            return true
        }

        fun tryAuthServiceDispose(): Boolean {
            authorizationService.get()?.let {
                authorizationService.set(null)
                it.dispose()
                return true
            }
            return false
        }
    }

    /*
    Extends AuthState class to update the shared preferences when an update is made
    with AuthorizationResponse
     */
    private fun AuthState.update(authResp: AuthorizationResponse?, authEx: AuthorizationException?, ctx: Context) {
        update(authResp, authEx)
        ctx.getSharedPreferences("auth", Context.MODE_PRIVATE).edit()
            .putString("stateJson", jsonSerializeString()).apply()
    }

    /*
    Extends AuthState class to update the shared preferences when an update is made
    with TokenResponse
     */
    private fun AuthState.update(tokenResp: TokenResponse?, authEx: AuthorizationException?, ctx: Context) {
        update(tokenResp, authEx)
        ctx.getSharedPreferences("auth", Context.MODE_PRIVATE).edit()
            .putString("stateJson", jsonSerializeString()).apply()
    }

    /*
    Extends AuthState class to update the shared preferences when an update is made
    with RegistrationResponse
     */
    private fun AuthState.update(regResp: RegistrationResponse, ctx: Context) {
        update(regResp)
        ctx.getSharedPreferences("auth", Context.MODE_PRIVATE).edit()
            .putString("stateJson", jsonSerializeString()).apply()
    }

    private val authState: AuthState
        get() {
            val stateJson = authSharedPreferences.getString("stateJson", null)
            return if (stateJson != null) {
                AuthState.jsonDeserialize(stateJson)
            } else AuthState()
        }

    val idToken: String?
        get() = authState.idToken

    val accessToken: String?
        get() = authState.accessToken

    val couldBeAuthorized: Boolean
        get() = authState.lastTokenResponse != null

    fun updateAuthWithIntent(intent: Intent, callback: (AuthorizationException?) -> Unit) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        authState.let {
            if (resp != null) {
                it.update(resp, ex, activity)
                authorizationService.get().performTokenRequest(
                    resp.createTokenExchangeRequest()
                ) { response, ex_new ->
                    if (response != null) {
                        it.update(response, ex_new, activity)
                    }
                    callback(ex_new)
                }
            } else {
                callback(ex)
            }
        }
    }

    fun performActionWithFreshTokens(callback: (String, String) -> Unit, errorCallback: (AuthorizationException?) -> Unit, force: Boolean = false) {
        authState.let{
            when {
                it.needsTokenRefresh || force -> {
                    val refreshRequest = it.createTokenRefreshRequest()
                    authorizationService.get().performTokenRequest(refreshRequest) { response, ex ->
                        if (response != null) {
                            it.update(response, ex, activity)
                            callback(it.accessToken!!, it.idToken!!)
                        } else {
                            errorCallback(ex)
                        }
                    }
                }
                it.isAuthorized -> {
                    callback(it.accessToken!!, it.idToken!!)
                }
                else -> {
                    errorCallback(null)
                }
            }
        }
    }

    fun getAuthorizationRequestIntent(request: AuthorizationRequest): Intent {
        return authorizationService.get().getAuthorizationRequestIntent(request)
    }

    fun deleteAuth(): AuthStateWrapper {
        authSharedPreferences.edit().remove("stateJson").apply()

        if (affiliationsSharedPreferences.contains("affiliations_array")) {
            affiliationsSharedPreferences.edit().remove("affiliations_array").apply()
        }
        return this
    }

    fun showAuthenticationError() {
        deleteAuth()
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("LOGGED_OUT", true)
        activity.startActivity(intent)
        activity.finish()
    }
}