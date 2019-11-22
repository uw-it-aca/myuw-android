package com.example.myuw

import android.content.res.Resources
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import androidx.core.content.res.ResourcesCompat

object UserInfoStore {
    private var menuItems = mapOf(
        "Home" to Pair(R.id.nav_home, R.drawable.ic_home_black_24dp),
        "Academics" to Pair(R.id.nav_academics, R.drawable.ic_menu_gallery), // TODO: add proper icon
        "Husky Experience" to Pair(R.id.nav_husky_experience, R.drawable.ic_menu_gallery), // TODO: add proper icon
        "Accounts" to Pair(R.id.nav_accounts, R.drawable.ic_menu_gallery), // TODO: add proper icon
        "Notice" to Pair(R.id.nav_notices, R.drawable.ic_menu_gallery), // TODO: add proper icon
        "Profile" to Pair(R.id.nav_profile, R.drawable.ic_account),
        "Academic Calendar" to Pair(R.id.nav_academic_calendar, R.drawable.ic_menu_gallery), // TODO: add proper icon
        "Resources" to Pair(R.id.nav_resources, R.drawable.ic_menu_gallery) // TODO: add proper icon
    )

    private var activeMenuItems = menuItems

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
}