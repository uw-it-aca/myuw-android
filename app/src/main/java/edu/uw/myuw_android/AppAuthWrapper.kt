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

class AppAuthWrapper(private val activity: Activity) {
    private val authSharedPreferences: SharedPreferences =
        activity.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val affiliationsSharedPreferences =
        activity.getSharedPreferences("affiliations", Context.MODE_PRIVATE)
    private val resources: Resources = activity.resources

    companion object {
        private var instanceCount = 0
        private lateinit var authorizationService: AuthorizationService
    }

    init {
        instanceCount++
        if (instanceCount == 1)
            authorizationService = AuthorizationService(activity)
    }

    var authState: AuthState?
        get() {
            val stateJson = authSharedPreferences.getString("stateJson", null)
            return if (stateJson != null) {
                AuthState.jsonDeserialize(stateJson)
            } else null
        }
        private set(newAuthState) {
            if (newAuthState != null) {
                authSharedPreferences.edit()
                    .putString("stateJson", newAuthState.jsonSerializeString())
                    .apply()
            } else {
                activity.getSharedPreferences("affiliations", Context.MODE_PRIVATE).edit().remove("affiliations_array").apply()
                authSharedPreferences.edit().remove("stateJson").apply()
            }
        }

    val couldBeAuthorized: Boolean
        get() = authState != null

    fun updateAuthWithIntent(intent: Intent, callback: (AuthorizationException?) -> Unit) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        authState = AuthState()

        authState?.let {
            if (resp != null) {
                it.update(resp, ex)
                authorizationService.performTokenRequest(
                    resp.createTokenExchangeRequest()
                ) { response, ex_new ->
                    if (response != null) {
                        it.update(response, ex_new)
                        authState = it
                    }
                    callback(ex_new)
                }
            } else {
                callback(ex)
            }
        } ?: callback(null)
    }

    fun performActionWithFreshTokens(callback: (String, String) -> Unit, errorCallback: (AuthorizationException?) -> Unit) {
        authState?.let{
            when {
                it.needsTokenRefresh -> {
                    val refreshRequest = it.createTokenRefreshRequest()
                    authorizationService.performTokenRequest(refreshRequest) { response, ex ->
                        if (response != null) {
                            it.update(response, ex)
                            authState = it
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
        } ?: errorCallback(null)
    }

    fun getAuthorizationRequestIntent(request: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(request)
    }

    fun deleteAuth(): AppAuthWrapper {
        authState = null
        if (affiliationsSharedPreferences.contains("affiliations_array")) {
            affiliationsSharedPreferences.edit().remove("affiliations_array").apply()
        }
        return this
    }

    fun onDestroy() {
        if (instanceCount == 1)
            authorizationService.dispose()
        instanceCount--
    }

    fun showAuthenticationError() {
        ErrorActivity.showError(
            resources.getString(R.string.sign_in_error),
            resources.getString(R.string.sign_in_error_desc),
            resources.getString(R.string.onReceiveErrorButton),
            ErrorActivity.ErrorHandlerEnum.RETRY_LOGIN,
            activity
        )
    }
}