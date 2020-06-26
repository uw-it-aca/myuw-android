package edu.uw.myuw_android

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import android.view.Menu
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import edu.my.myuw_android.R
import net.openid.appauth.AuthState
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.CookieHandler
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object UserInfoStore {
    private var menuItems = mapOf(
        R.id.group_nav_drawer_main to mapOf<String, Triple<Int, Int, (Set<String>) -> Boolean>>(
            "Home" to Triple(
                R.id.nav_home,
                R.drawable.ic_home_black_24dp,
                { _ -> true }
            ),
            "Academics" to Triple(
                R.id.nav_academics,
                R.drawable.ic_action_academics,
                { aff: Set<String> -> aff.contains("student") or aff.contains("applicant") }
            ),
            "Husky Experience" to Triple(
                R.id.nav_husky_experience,
                R.drawable.ic_husky_experience,
                { aff: Set<String> -> (aff.contains("undergrad") and aff.contains("seattle")) or aff.contains("hxt_viewer") }
            ),
            "Teaching" to Triple(
                R.id.nav_teaching,
                R.drawable.ic_edit_black_24dp,
                { aff: Set<String> -> aff.contains("instructor") }
            ),
            "Accounts" to Triple(
                R.id.nav_accounts,
                R.drawable.ic_accounts,
                { _ -> true }
            ),
            "Notices" to Triple(
                R.id.nav_notices,
                R.drawable.ic_notices,
                { aff: Set<String> -> aff.contains("student") }
            ),
            "Profile" to Triple(
                R.id.nav_profile,
                R.drawable.ic_baseline_person_24,
                { _ -> true }
            ),
            "Calendar" to Triple(
                R.id.nav_academic_calendar,
                R.drawable.ic_calander,
                { _ -> true }
            ),
            "UW Resources" to Triple(
                R.id.nav_resources,
                R.drawable.ic_resources,
                { _ -> true }
            )
        ),
        R.id.group_nav_drawer_secondary to mapOf(
            "Sign out" to Triple(
                R.id.logout,
                R.drawable.ic_exit_to_app_black_24dp,
                { _ -> true }
            )
        )
    )

    private var activeMenuItems = menuItems
    private var affiliations = mutableSetOf<String>()

    var name: MutableLiveData<String> = MutableLiveData()
    var email: MutableLiveData<String>  = MutableLiveData()
    var netId: MutableLiveData<String> = MutableLiveData()
    var emailForwardUrl: MutableLiveData<String> = MutableLiveData()
    var groups: MutableLiveData<Set<String>> = MutableLiveData()

    fun setNavigationMenu(menu: Menu, context: Context) {
        menu.clear()

        Log.d("setNavigationMenu - affiliations: ", affiliations.toString())
        for (key in activeMenuItems.keys) {
            val currentGroup = activeMenuItems.getValue(key)

            menu.setGroupCheckable(key, true, true)
            for (title in currentGroup.keys) {
                Log.d("setNavigationMenu - $title", currentGroup.getValue(title).third(affiliations).toString())
                if (currentGroup.getValue(title).third(affiliations)) {
                    val menuItem = menu.add(
                        key,
                        currentGroup.getValue(title).first,
                        Menu.NONE,
                        title
                    )
                    menuItem.icon = ResourcesCompat.getDrawable(
                        context.resources,
                        currentGroup.getValue(title).second,
                        null
                    )
                }
            }
        }
    }

    fun updateAffiliations(activity: Activity, resources: Resources, authService: AppAuthWrapper) {
        val affiliationsSharedPreferences = activity.getSharedPreferences("affiliations", Context.MODE_PRIVATE)
        if (affiliationsSharedPreferences.contains("affiliations_array")) {
            affiliations = affiliationsSharedPreferences.getStringSet("affiliations_array", null)!!
        } else {
            try {
                val makeRequest: (idToken: String?) -> HttpURLConnection = { idToken ->
                    (URL(resources.getString(R.string.myuw_affiliation_endpoint)).openConnection() as HttpURLConnection).let {
                        it.setRequestProperty(
                            "Authorization",
                            "Bearer $idToken"
                        )
                        it.requestMethod = "GET"
                        it.useCaches = false
                        it.connect()
                        it
                    }
                }

                var conn = makeRequest(authService.authState!!.idToken)

                if (conn.responseCode == 401)
                    authService.performActionWithFreshTokens({ _, idToken ->
                        conn = makeRequest(idToken)
                    }, {
                        authService.showAuthenticationError()
                    }, true)

                var responseJson = ""

                BufferedReader(
                    InputStreamReader(
                        conn.inputStream,
                        StandardCharsets.UTF_8
                    )
                ).forEachLine {
                    responseJson += it + '\n'
                }

                conn.disconnect()
                affiliations.clear()

                Log.d("updateAffiliations - responseJson: ", responseJson)
                val decodedResponse = JSONObject(responseJson)
                decodedResponse.keys().forEach {
                    if (decodedResponse[it] is Boolean && (decodedResponse[it] as Boolean)) {
                        affiliations.add(it)
                    }
                }
                affiliationsSharedPreferences.edit()
                    .putStringSet("affiliations_array", affiliations).apply()
                Log.d("updateAffiliations - decodedRespose: ", responseJson)
                Log.d("updateAffiliations - affiliations: ", affiliations.toString())
            } catch (e: Exception) {
                Log.e("updateAffiliations - http error", e.toString())
                InternetCheck {
                    if (it) {
                        authService.onDestroy()
                        ErrorActivity.showError(
                            resources.getString(R.string.error_affiliations_update),
                            resources.getString(R.string.error_affilications_update_desc),
                            resources.getString(R.string.onReceiveErrorButton),
                            ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                            activity
                        )
                    } else {
                        authService.onDestroy()
                        ErrorActivity.showError(
                            resources.getString(R.string.no_internet),
                            resources.getString(R.string.no_internet_desc),
                            resources.getString(R.string.onReceiveErrorButton),
                            ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                            activity
                        )
                    }
                }
            }
        }
    }
}