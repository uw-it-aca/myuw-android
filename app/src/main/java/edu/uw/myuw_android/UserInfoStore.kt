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
import java.net.URL
import java.nio.charset.StandardCharsets

object UserInfoStore {
    private var menuItems = mapOf(
        R.id.group_nav_drawer_main to mapOf<String, Triple<Int, Int, (List<String>) -> Boolean>>(
            "Home" to Triple(
                R.id.nav_home,
                R.drawable.ic_home_black_24dp,
                { _ -> true }
            ),
            "Academics" to Triple(
                R.id.nav_academics,
                R.drawable.ic_action_academics,
                { aff: List<String> -> aff.contains("student") or aff.contains("applicant") }
            ),
            "Husky Experience" to Triple(
                R.id.nav_husky_experience,
                R.drawable.ic_husky_experience,
                { aff: List<String> -> (aff.contains("undergrad") and aff.contains("seattle")) or aff.contains("hxt_viewer") }
            ),
            "Teaching" to Triple(
                R.id.nav_teaching,
                R.drawable.ic_edit_black_24dp,
                { aff: List<String> -> aff.contains("instructor") }
            ),
            "Accounts" to Triple(
                R.id.nav_accounts,
                R.drawable.ic_accounts,
                { _ -> true }
            ),
            "Notices" to Triple(
                R.id.nav_notices,
                R.drawable.ic_notices,
                { aff: List<String> -> aff.contains("student") }
            ),
            "Profile" to Triple(
                R.id.nav_profile,
                R.drawable.ic_account,
                { _ -> true }
            ),
            "Calendar" to Triple(
                R.id.nav_academic_calendar,
                R.drawable.ic_calander,
                { _: List<String> -> true }
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
    private var affiliations = mutableListOf<String>()

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

    fun updateAffiliations(activity: Activity, resources: Resources, idToken: String) {
        try {
            val conn =
                URL(resources.getString(R.string.myuw_affiliation_endpoint)).openConnection()
            conn.setRequestProperty("Authorization", "Bearer $idToken")
            var responseJson = ""
            BufferedReader(
                InputStreamReader(
                    conn.getInputStream(),
                    StandardCharsets.UTF_8
                )
            ).forEachLine {
                responseJson += it + '\n'
            }

            Log.d("updateAffiliations - responseJson: ", responseJson)
            val decodedResponse = JSONObject(responseJson)
            decodedResponse.keys().forEach {
                if (decodedResponse[it] is Boolean && (decodedResponse[it] as Boolean)) {
                    affiliations.add(it)
                }
            }
            Log.d("updateAffiliations - decodedRespose: ", responseJson)
            Log.d("updateAffiliations - affiliations: ", affiliations.toString())
        }
        catch (e: Exception) {
            Log.e("updateAffiliations - http error", e.toString())
            InternetCheck {
                if (it) {
                    ErrorActivity.showError(
                        "Unable to Update Affiliations",
                        "A server error has occurred. We are aware of this issue and are working on it. Please try again in a few minutes. This message needs to be updated by ux",
                        "Retry",
                        ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                        activity
                    )
                } else {
                    ErrorActivity.showError(
                        "No Internet Connection",
                        "Please connect to internet. This message needs to be updated by ux",
                        "Retry",
                        ErrorActivity.ErrorHandlerEnum.RELOAD_PAGE,
                        activity
                    )
                }
            }

        }
    }

    fun readAuthState(context: Context): AuthState {
        val authPrefs: SharedPreferences =
            context.getSharedPreferences("auth", MODE_PRIVATE)
        val stateJson = authPrefs.getString("stateJson", null)
        return if (stateJson != null) {
            AuthState.jsonDeserialize(stateJson)
        } else {
            AuthState()
        }
    }

    fun writeAuthState(context: Context, state: AuthState) {
        val authPrefs: SharedPreferences =
            context.getSharedPreferences("auth", MODE_PRIVATE)
        authPrefs.edit()
            .putString("stateJson", state.jsonSerializeString())
            .apply()
    }
}