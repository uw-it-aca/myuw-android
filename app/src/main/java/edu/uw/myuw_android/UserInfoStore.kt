package edu.uw.myuw_android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Resources
import android.view.Menu
import android.webkit.JavascriptInterface
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import edu.my.myuw_android.R
import net.openid.appauth.AuthState


object UserInfoStore {
    private var menuItems = mapOf(
        R.id.group_nav_drawer_main to mapOf(
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
            "Calendar" to Pair(
                R.id.nav_academic_calendar,
                R.drawable.ic_calander
            ),
            "UW Resources" to Pair(
                R.id.nav_resources,
                R.drawable.ic_resources
            )
        ),
        R.id.group_nav_drawer_secondary to mapOf(
            "Signout" to Pair(
                R.id.logout,
                R.drawable.ic_exit_to_app_black_24dp
            )
        )
    )

    private var activeMenuItems = menuItems

    var name: MutableLiveData<String> = MutableLiveData()
    var email: MutableLiveData<String>  = MutableLiveData()
    var netId: MutableLiveData<String> = MutableLiveData()
    var emailForwardUrl: MutableLiveData<String> = MutableLiveData()
    var groups: MutableLiveData<Set<String>> = MutableLiveData()

    fun setNavigationMenu(menu: Menu, resources: Resources) {
        menu.clear()

        for (key in activeMenuItems.keys) {
            val currentGroup = activeMenuItems.getValue(key)

            menu.setGroupCheckable(key, true, true)
            for (title in currentGroup.keys) {
                val menuItem = menu.add(
                    key,
                    currentGroup.getValue(title).first,
                    Menu.NONE,
                    title
                )
                menuItem.icon = ResourcesCompat.getDrawable(
                    resources,
                    currentGroup.getValue(title).second,
                    null
                )
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