package edu.uw.myuw_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationRequest

class AppAuthWrapper(context: Context) {
    private val authSharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val authorizationService: AuthorizationService = AuthorizationService(context)

    var authState: AuthState
        get() {
            val stateJson = authSharedPreferences.getString("stateJson", null)
            return if (stateJson != null) {
                AuthState.jsonDeserialize(stateJson)
            } else {
                AuthState()
            }
        }
        private set(newAuthState) {
            authSharedPreferences.edit()
                .putString("stateJson", newAuthState.jsonSerializeString())
                .apply()
        }

    val isAuthorized: Boolean
        get() = authState.isAuthorized

    fun updateAuthWithIntent(intent: Intent, callback: (AuthorizationException?) -> Unit) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        if (resp != null) {
            authorizationService.performTokenRequest(
                resp.createTokenExchangeRequest()
            ) { response, ex_new ->
                if (response != null) {
                    authState.let {
                        it.update(response, ex)
                        authState = it
                    }
                }
                callback(ex_new)
            }
        } else {
            callback(ex)
        }
    }

    fun performActionWithFreshTokens(callback: (String, String) -> Unit, errorCallback: (AuthorizationException?) -> Unit) {
        when {
            authState.needsTokenRefresh -> {
                val refreshRequest = authState.createTokenRefreshRequest()
                authorizationService.performTokenRequest(refreshRequest) { response, ex ->
                    if (response != null) {
                        authState.let {
                            it.update(response, ex)
                            authState = it
                        }
                        callback(authState.accessToken!!, authState.idToken!!)
                    } else {
                        errorCallback(ex)
                    }
                }
            }
            authState.isAuthorized -> {
                callback(authState.accessToken!!, authState.idToken!!)
            }
            else -> {
                errorCallback(null)
            }
        }
    }

    fun getAuthorizationRequestIntent(request: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(request)
    }

    fun onDestroy() {
        authorizationService.dispose()
    }
}