package com.example.hotwheelscollector

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.SessionManager
import com.example.hotwheelscollector.ui.notifications.NotificationBottomSheet
import com.example.hotwheelscollector.ui.settings.CollectorSetupBottomSheet

class MainActivity : AppCompatActivity() {

    private var currentDestinationId: Int = R.id.nav_home

    private lateinit var db: DatabaseHelper
    private lateinit var notificationDot: View

    override fun onCreate(savedInstanceState: Bundle?) {

        SessionManager.getOrCreateGuestUser(this)

        // =========================
        // TEMA DINÁMICO (HOT WHEELS)
        // =========================
        val prefs = getSharedPreferences(
            "collector_prefs",
            MODE_PRIVATE
        )

        val theme = prefs.getString("theme", "red")

        when (theme) {

            "orange" -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )

            else -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        // =========================
        // ESTADO NAV
        // =========================
        if (savedInstanceState != null) {

            currentDestinationId =
                savedInstanceState.getInt(
                    "nav_state",
                    R.id.nav_home
                )
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        val prefsNav = getSharedPreferences(
            "collector_prefs",
            MODE_PRIVATE
        )

        val lastDestination =
            prefsNav.getInt(
                "last_destination",
                R.id.nav_home
            )

        if (
            savedInstanceState == null &&
            lastDestination != R.id.nav_home
        ) {

            navController.navigate(lastDestination)

            prefsNav.edit()
                .remove("last_destination")
                .apply()
        }

        // =========================
        // NAV ITEMS
        // =========================
        val home = findViewById<View>(R.id.nav_home)

        val collection =
            findViewById<View>(R.id.nav_collection)

        val favorites =
            findViewById<View>(R.id.nav_favorites)

        val profile =
            findViewById<View>(R.id.nav_profile)

        val indicator =
            findViewById<View>(R.id.nav_indicator)

        // =========================
        // TOP BAR
        // =========================
        val ivTune =
            findViewById<ImageView>(R.id.ivTune)

        val layoutNotifications =
            findViewById<View>(R.id.layoutNotifications)

        notificationDot =
            findViewById(R.id.viewNotificationDot)

        refreshNotificationDot()

        // =========================
        // CLICK NOTIFICATIONS
        // =========================
        layoutNotifications.setOnClickListener {

            NotificationBottomSheet()
                .show(
                    supportFragmentManager,
                    "notifications"
                )

            db.markAllNotificationsAsRead()

            refreshNotificationDot()
        }

        // =========================
        // CLICK SETTINGS
        // =========================
        ivTune.setOnClickListener {

            CollectorSetupBottomSheet()
                .show(
                    supportFragmentManager,
                    "collector_setup"
                )
        }

        // =========================
        // INDICADOR ANIMADO
        // =========================
        fun moveIndicator(selected: View) {

            selected.post {

                val width = selected.width
                val x = selected.x

                if (indicator.layoutParams.width != width) {

                    indicator.layoutParams.width = width
                    indicator.requestLayout()
                }

                indicator.animate()
                    .x(x)
                    .setDuration(120)
                    .setInterpolator(
                        DecelerateInterpolator()
                    )
                    .start()
            }
        }

        // =========================
        // SELECCIÓN VISUAL NAV
        // =========================
        fun selectItem(selected: View) {

            val activeColor =
                getColor(R.color.icon_active)

            val inactiveColor =
                getColor(R.color.icon_inactive)

            val icons = listOf(
                R.id.icon_home,
                R.id.icon_collection,
                R.id.icon_favorites,
                R.id.icon_profile
            )

            val texts = listOf(
                R.id.text_home,
                R.id.text_collection,
                R.id.text_favorites,
                R.id.text_profile
            )

            val itemIds = listOf(
                R.id.nav_home,
                R.id.nav_collection,
                R.id.nav_favorites,
                R.id.nav_profile
            )

            // RESET
            itemIds.forEachIndexed { index, id ->

                val item = findViewById<View>(id)

                item.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()

                findViewById<ImageView>(icons[index])
                    .setColorFilter(inactiveColor)

                findViewById<TextView>(texts[index])
                    .setTextColor(inactiveColor)
            }

            // ACTIVO
            selected.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(120)
                .setInterpolator(
                    DecelerateInterpolator()
                )
                .start()

            when (selected.id) {

                R.id.nav_home -> {

                    findViewById<ImageView>(
                        R.id.icon_home
                    ).setColorFilter(activeColor)

                    findViewById<TextView>(
                        R.id.text_home
                    ).setTextColor(activeColor)
                }

                R.id.nav_collection -> {

                    findViewById<ImageView>(
                        R.id.icon_collection
                    ).setColorFilter(activeColor)

                    findViewById<TextView>(
                        R.id.text_collection
                    ).setTextColor(activeColor)
                }

                R.id.nav_favorites -> {

                    findViewById<ImageView>(
                        R.id.icon_favorites
                    ).setColorFilter(activeColor)

                    findViewById<TextView>(
                        R.id.text_favorites
                    ).setTextColor(activeColor)
                }

                R.id.nav_profile -> {

                    findViewById<ImageView>(
                        R.id.icon_profile
                    ).setColorFilter(activeColor)

                    findViewById<TextView>(
                        R.id.text_profile
                    ).setTextColor(activeColor)
                }
            }

            moveIndicator(selected)
        }

        // =========================
        // NAVEGACIÓN SEGURA
        // =========================
        fun navigateSafe(destination: Int) {

            navController.navigate(
                destination,
                null,
                navOptions {

                    popUpTo(
                        navController.graph.startDestinationId
                    )

                    launchSingleTop = true
                }
            )
        }

        // =========================
        // LISTENER NAV
        // =========================
        navController.addOnDestinationChangedListener {
                _, destination, _ ->

            currentDestinationId = destination.id

            when (destination.id) {

                R.id.nav_home -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.VISIBLE

                    refreshNotificationDot()
                }

                R.id.nav_collection -> {

                    ivTune.visibility = View.VISIBLE

                    layoutNotifications.visibility =
                        View.GONE
                }

                R.id.nav_favorites -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE
                }

                R.id.nav_profile -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE
                }

                else -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE
                }
            }

            when (destination.id) {

                R.id.nav_home ->
                    selectItem(home)

                R.id.nav_collection ->
                    selectItem(collection)

                R.id.nav_favorites ->
                    selectItem(favorites)

                R.id.nav_profile ->
                    selectItem(profile)
            }
        }

        // =========================
        // CLICKS NAV
        // =========================
        home.setOnClickListener {
            navigateSafe(R.id.nav_home)
        }

        collection.setOnClickListener {
            navigateSafe(R.id.nav_collection)
        }

        favorites.setOnClickListener {
            navigateSafe(R.id.nav_favorites)
        }

        profile.setOnClickListener {
            navigateSafe(R.id.nav_profile)
        }

        // =========================
        // ESTADO INICIAL
        // =========================
        home.post {

            when (currentDestinationId) {

                R.id.nav_home -> {

                    selectItem(home)

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.VISIBLE

                    refreshNotificationDot()
                }

                R.id.nav_collection -> {

                    selectItem(collection)

                    ivTune.visibility = View.VISIBLE

                    layoutNotifications.visibility =
                        View.GONE
                }

                R.id.nav_favorites -> {

                    selectItem(favorites)

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE
                }

                R.id.nav_profile -> {

                    selectItem(profile)

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE
                }
            }
        }
    }

    // =========================
    // REFRESH DOT
    // =========================
    fun refreshNotificationDot() {

        notificationDot.visibility =
            if (db.hasUnreadNotifications())
                View.VISIBLE
            else
                View.GONE
    }
}