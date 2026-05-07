package com.example.hotwheelscollector

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions

class MainActivity : AppCompatActivity() {

    private var currentDestinationId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            currentDestinationId = savedInstanceState.getInt("nav_state", R.id.nav_home)
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        //NAV ITEMS
        val home = findViewById<View>(R.id.nav_home)
        val collection = findViewById<View>(R.id.nav_collection)
        val favorites = findViewById<View>(R.id.nav_favorites)
        val profile = findViewById<View>(R.id.nav_profile)

        val indicator = findViewById<View>(R.id.nav_indicator)

        //INDICADOR
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
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        //SELECCIÓN VISUAL
        fun selectItem(selected: View) {

            val activeColor = getColor(R.color.icon_active)
            val inactiveColor = getColor(R.color.icon_inactive)

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

            //RESET
            itemIds.forEachIndexed { index, id ->
                val item = findViewById<View>(id)

                item.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()

                findViewById<ImageView>(icons[index]).setColorFilter(inactiveColor)
                findViewById<TextView>(texts[index]).setTextColor(inactiveColor)
            }

            //ACTIVO
            selected.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(120)
                .setInterpolator(DecelerateInterpolator())
                .start()

            when (selected.id) {
                R.id.nav_home -> {
                    findViewById<ImageView>(R.id.icon_home).setColorFilter(activeColor)
                    findViewById<TextView>(R.id.text_home).setTextColor(activeColor)
                }
                R.id.nav_collection -> {
                    findViewById<ImageView>(R.id.icon_collection).setColorFilter(activeColor)
                    findViewById<TextView>(R.id.text_collection).setTextColor(activeColor)
                }
                R.id.nav_favorites -> {
                    findViewById<ImageView>(R.id.icon_favorites).setColorFilter(activeColor)
                    findViewById<TextView>(R.id.text_favorites).setTextColor(activeColor)
                }
                R.id.nav_profile -> {
                    findViewById<ImageView>(R.id.icon_profile).setColorFilter(activeColor)
                    findViewById<TextView>(R.id.text_profile).setTextColor(activeColor)
                }
            }

            moveIndicator(selected)
        }

        //Navegación segura
        fun navigateSafe(destination: Int) {
            navController.navigate(
                destination,
                null,
                navOptions {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            )
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->

            currentDestinationId = destination.id

            when (destination.id) {
                R.id.nav_home -> selectItem(home)
                R.id.nav_collection -> selectItem(collection)
                R.id.nav_favorites -> selectItem(favorites)
                R.id.nav_profile -> selectItem(profile)
            }
        }

        home.setOnClickListener { navigateSafe(R.id.nav_home) }
        collection.setOnClickListener { navigateSafe(R.id.nav_collection) }
        favorites.setOnClickListener { navigateSafe(R.id.nav_favorites) }
        profile.setOnClickListener { navigateSafe(R.id.nav_profile) }

        home.post {
            when (currentDestinationId) {
                R.id.nav_home -> selectItem(home)
                R.id.nav_collection -> selectItem(collection)
                R.id.nav_favorites -> selectItem(favorites)
                R.id.nav_profile -> selectItem(profile)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("nav_state", currentDestinationId)
    }
}