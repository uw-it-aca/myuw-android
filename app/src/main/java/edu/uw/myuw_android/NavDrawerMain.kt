package edu.uw.myuw_android

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import edu.my.myuw_android.R
import net.openid.appauth.AuthState

class NavDrawerMain : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        UserInfoStore.setNavigationMenu(navView.menu, resources)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_academics,
                R.id.nav_husky_experience,
                R.id.nav_accounts,
                R.id.nav_notices,
                R.id.nav_profile,
                R.id.nav_academic_calendar,
                R.id.nav_resources,
                R.id.logout
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.getHeaderView(0).setOnClickListener{
            navController.navigate(R.id.nav_profile)
            navView.setCheckedItem(R.id.nav_profile)
            drawerLayout.closeDrawer(navView)
        }

        UserInfoStore.name.observe(this, Observer {
            navView.getHeaderView(0).findViewById<TextView>(R.id.drawer_username).text = it
        })

        UserInfoStore.email.observe(this, Observer {
            navView.getHeaderView(0).findViewById<TextView>(R.id.drawer_email).text = it
        })

        navView.setNavigationItemSelectedListener {
            Log.d("NavDrawerMain - onCreate", it.title.toString())
            when (it.itemId) {
                R.id.logout -> {
                    Log.d("NavDrawerMain - logout", "logging out user")
                    val currentState = UserInfoStore.readAuthState(this)
                    val clearedState = AuthState(currentState.authorizationServiceConfiguration!!)
                    if (currentState.lastRegistrationResponse != null) {
                        clearedState.update(currentState.lastRegistrationResponse)
                    }
                    UserInfoStore.writeAuthState(this, clearedState)
                    val mainIntent = Intent(this, LoginActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(mainIntent)
                    finish()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(it, navController)
                    if (handled) drawerLayout.closeDrawer(navView)
                    handled
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.nav_drawer_main, menu)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu.findItem(R.id.action_open_search).actionView as SearchView).apply {
            // Assumes current activity is the searchable activity
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("NavDrawerMain - onOptionsItemSelected", item.toString())
        return when (item.itemId) {
            R.id.action_open_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments[0] as CommonWebViewFragment
        if (currentFragment.onBackPress()) super.onBackPressed()
    }
}
