package edu.uw.myuw_android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Resources
import android.view.Menu
import android.webkit.JavascriptInterface
import androidx.core.content.res.ResourcesCompat
import edu.my.myuw_android.R
import net.openid.appauth.AuthState


object UserInfoStore {
    private var menuItems = mapOf(
        "Home" to Pair(
            R.id.nav_home,
            R.drawable.ic_home_black_24dp
        ),
        "Academics" to Pair(
            R.id.nav_academics,
            R.drawable.ic_action_academics
        ),
        "Husky Experience" to Pair(
            R.id.nav_husky_experience,
            R.drawable.ic_husky_experience
        ),
        "Accounts" to Pair(
            R.id.nav_accounts,
            R.drawable.ic_accounts
        ),
        "Notices" to Pair(
            R.id.nav_notices,
            R.drawable.ic_notices
        ),
        "Profile" to Pair(
            R.id.nav_profile,
            R.drawable.ic_account
        ),
        "Academic Calendar" to Pair(
            R.id.nav_academic_calendar,
            R.drawable.ic_calander
        ),
        "Resources" to Pair(
            R.id.nav_resources,
            R.drawable.ic_resources
        )
    )

    private var activeMenuItems = menuItems

    var name: String = "John Average"
    var email: String = "javerage@uw.edu"
    var netId: String = ""
    var emailForwardUrl: String = ""
    var groups: Set<String> = HashSet<String>()

    @JavascriptInterface
    fun decodeUserStream(token: String, tokenValue: String) {
        when {
            tokenValue == "true" -> groups.plus(token)
            token == "netid" -> netId = tokenValue
            token == "email_forward_url" -> emailForwardUrl = tokenValue
        }
    }

    fun setNavigationMenu(menu: Menu, resources: Resources) {
        menu.clear()

        for (key in activeMenuItems.keys) {
            val menuItem = menu.add(R.id.group_nav_drawer_main, menuItems.getValue(key).first, Menu.NONE, key)
            menuItem.icon = ResourcesCompat.getDrawable(resources, menuItems.getValue(key).second, null)
        }

        menu.setGroupCheckable(R.id.group_nav_drawer_main, true, true)
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