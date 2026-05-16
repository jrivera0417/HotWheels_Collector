package com.example.hotwheelscollector

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.hotwheelscollector.data.backup.FileExporter
import com.example.hotwheelscollector.data.backup.FileImporter
import com.example.hotwheelscollector.data.cloud.FirestoreManager
import com.example.hotwheelscollector.data.cloud.SyncState
import com.example.hotwheelscollector.data.cloud.SyncStatusManager
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.example.hotwheelscollector.data.network.NetworkMonitor
import com.example.hotwheelscollector.data.notifications.AppNotification
import com.example.hotwheelscollector.ui.notifications.NotificationBottomSheet
import com.example.hotwheelscollector.ui.settings.CollectorSetupBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {

                FileImporter(this)
                    .importCollection(uri)
            }
        }

    private var currentDestinationId: Int = R.id.nav_home

    private lateinit var db: DatabaseHelper
    private lateinit var notificationDot: View
    private lateinit var imgSyncStatus: ImageView
    private lateinit var ivMenu: ImageView

    // =========================
    // FIREBASE AUTH
    // =========================
    private lateinit var auth: FirebaseAuth
    private lateinit var authListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {

        // =========================
        // TEMA DINÁMICO
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

        // =========================
        // FIREBASE AUTH
        // =========================
        auth = FirebaseAuth.getInstance()

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            val user = firebaseAuth.currentUser

            Log.d(
                "AUTH_LISTENER",
                "Usuario restaurado: ${user?.email}"
            )

            refreshTopBarState()
        }

        // =========================
        // DATABASE
        // =========================
        db = DatabaseHelper(this)

        // =========================
        // GUEST USER
        // =========================
        SessionManager.getOrCreateGuestUser(this)

        // =========================
        // FIREBASE SESSION
        // =========================
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        Log.d(
            "SESSION_DEBUG",
            "Firebase User = ${firebaseUser?.email}"
        )

        // =========================
        // RESTORE SQLITE USER
        // =========================
        if (firebaseUser != null) {

            var localUser =
                db.getUserByFirebaseUid(
                    firebaseUser.uid
                )

            if (localUser == null) {

                db.insertUser(
                    com.example.hotwheelscollector.data.models.User(
                        name = firebaseUser.displayName
                            ?: "Usuario",

                        email = firebaseUser.email
                            ?: "",

                        password = "",

                        firebaseUid = firebaseUser.uid
                    )
                )
            }

            // =========================
            // RESTORE CLOUD
            // =========================
            FirestoreManager(this)
                .restoreFromCloud(
                    onSuccess = {},
                    onError = {}
                )
        }

        val networkMonitor = NetworkMonitor(this)

        networkMonitor.registerCallback(

            onConnected = {

                runOnUiThread {

                    SyncStatusManager.setState(
                        SyncState.SYNCING
                    )

                    val currentFirebaseUser =
                        FirebaseAuth.getInstance().currentUser

                    if (currentFirebaseUser != null) {

                        FirestoreManager(this)
                            .syncAllToCloud(

                                onSuccess = {

                                    runOnUiThread {

                                        SyncStatusManager.setState(
                                            SyncState.SYNCED
                                        )

                                        db.insertNotification(
                                            AppNotification(
                                                title = "Sincronización completada",
                                                message = "Tu colección fue sincronizada correctamente con la nube.",
                                                timestamp = System.currentTimeMillis(),
                                                isRead = false
                                            )
                                        )

                                        refreshNotificationDot()
                                    }
                                },

                                onError = {

                                    runOnUiThread {

                                        SyncStatusManager.setState(
                                            SyncState.PENDING
                                        )
                                    }
                                }
                            )

                    } else {

                        SyncStatusManager.setState(
                            SyncState.SYNCED
                        )
                    }
                }
            },

            onDisconnected = {

                runOnUiThread {

                    SyncStatusManager.setState(
                        SyncState.OFFLINE
                    )
                }
            }
        )

        if (!networkMonitor.isConnected()) {

            SyncStatusManager.setState(
                SyncState.OFFLINE
            )
        }

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

        val collection = findViewById<View>(R.id.nav_collection)

        val favorites = findViewById<View>(R.id.nav_favorites)

        val profile = findViewById<View>(R.id.nav_profile)

        val indicator = findViewById<View>(R.id.nav_indicator)

        // =========================
        // TOP BAR
        // =========================
        val ivTune = findViewById<ImageView>(R.id.ivTune)

        val layoutNotifications =
            findViewById<View>(R.id.layoutNotifications)

        imgSyncStatus =
            findViewById(R.id.imgSyncStatus)

        ivMenu =
            findViewById(R.id.ivMenu)

        notificationDot =
            findViewById(R.id.viewNotificationDot)

        refreshNotificationDot()

        SyncStatusManager.observe { state ->

            runOnUiThread {

                updateSyncIcon(state)
            }
        }

        // =========================
        // CLICK CLOUD STATUS
        // =========================
        imgSyncStatus.setOnClickListener {

            val message = when (
                SyncStatusManager.getState()
            ) {

                SyncState.SYNCED -> {
                    "Colección sincronizada correctamente"
                }

                SyncState.PENDING -> {
                    "Hay cambios pendientes por sincronizar"
                }

                SyncState.SYNCING -> {
                    "Sincronizando colección..."
                }

                SyncState.OFFLINE -> {
                    "Sin conexión a internet"
                }

                SyncState.ERROR -> {
                    "Error de sincronización"
                }
            }

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }

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
        // CLICK MENU
        // =========================
        ivMenu.setOnClickListener { view ->

            val popupMenu =
                PopupMenu(this, view)

            popupMenu.menuInflater.inflate(
                R.menu.topbar_menu,
                popupMenu.menu
            )

            for (i in 0 until popupMenu.menu.size()) {

                val item = popupMenu.menu.getItem(i)

                val spannable =
                    android.text.SpannableString(item.title)

                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(
                        getColor(R.color.text_primary)
                    ),
                    0,
                    spannable.length,
                    0
                )

                item.title = spannable
            }

            val isLoggedIn =
                FirebaseAuth.getInstance().currentUser != null

            popupMenu.menu
                .findItem(R.id.menu_statistics)
                ?.isVisible = isLoggedIn

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.itemId) {

                    R.id.menu_export -> {

                        FileExporter(this)
                            .exportCollection()

                        true
                    }

                    R.id.menu_import -> {

                        importLauncher.launch(
                            "application/json"
                        )

                        true
                    }

                    R.id.menu_statistics -> {

                        if (FirebaseAuth.getInstance().currentUser != null) {

                            navController.navigate(
                                R.id.statisticsFragment
                            )

                        } else {

                            Toast.makeText(
                                this,
                                "Disponible solo para usuarios registrados",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        true
                    }

                    R.id.menu_about -> {

                        navController.navigate(
                            R.id.aboutFragment
                        )

                        true
                    }

                    R.id.menu_privacy -> {

                        navController.navigate(
                            R.id.privacyFragment
                        )

                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
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

            itemIds.forEachIndexed { index, id ->

                val item =
                    findViewById<View>(id)

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

                    refreshTopBarState()

                    refreshNotificationDot()
                }

                R.id.nav_collection -> {

                    ivTune.visibility = View.VISIBLE

                    layoutNotifications.visibility =
                        View.GONE

                    refreshTopBarState()
                }

                R.id.nav_favorites -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE

                    imgSyncStatus.visibility = View.GONE
                }

                R.id.nav_profile -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE

                    imgSyncStatus.visibility = View.GONE
                }

                else -> {

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE

                    imgSyncStatus.visibility = View.GONE
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

                    refreshTopBarState()

                    refreshNotificationDot()
                }

                R.id.nav_collection -> {

                    selectItem(collection)

                    ivTune.visibility = View.VISIBLE

                    layoutNotifications.visibility =
                        View.GONE

                    refreshTopBarState()
                }

                R.id.nav_favorites -> {

                    selectItem(favorites)

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE

                    imgSyncStatus.visibility = View.GONE
                }

                R.id.nav_profile -> {

                    selectItem(profile)

                    ivTune.visibility = View.GONE

                    layoutNotifications.visibility =
                        View.GONE

                    imgSyncStatus.visibility = View.GONE
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

    // =========================
    // REFRESH TOP BAR
    // =========================
    private fun refreshTopBarState() {

        if (!::imgSyncStatus.isInitialized) {
            return
        }

        val isLoggedIn =
            FirebaseAuth.getInstance().currentUser != null

        imgSyncStatus.visibility =
            if (isLoggedIn)
                View.VISIBLE
            else
                View.GONE
    }

    // =========================
    // SYNC ICON
    // =========================
    private fun updateSyncIcon(
        state: SyncState
    ) {

        val firebaseUser: FirebaseUser? =
            FirebaseAuth.getInstance().currentUser

        if (firebaseUser == null) {

            imgSyncStatus.visibility = View.GONE
            return
        }

        imgSyncStatus.visibility = View.VISIBLE

        imgSyncStatus.animate().cancel()
        imgSyncStatus.rotation = 0f

        when (state) {

            SyncState.PENDING -> {

                imgSyncStatus.setImageResource(
                    R.drawable.ic_cloud_upload
                )
            }

            SyncState.SYNCING -> {

                imgSyncStatus.animate().cancel()

                imgSyncStatus.setImageResource(
                    R.drawable.ic_cloud_sync
                )

                imgSyncStatus.animate()
                    .rotation(360f)
                    .setDuration(900)
                    .setInterpolator(
                        LinearInterpolator()
                    )
                    .withEndAction {

                        updateSyncIcon(
                            SyncStatusManager.getState()
                        )
                    }
                    .start()
            }

            SyncState.SYNCED -> {

                imgSyncStatus.setImageResource(
                    R.drawable.ic_cloud_done
                )
            }

            SyncState.OFFLINE -> {

                imgSyncStatus.setImageResource(
                    R.drawable.ic_cloud_off
                )
            }

            SyncState.ERROR -> {

                imgSyncStatus.setImageResource(
                    R.drawable.ic_cloud_off
                )
            }
        }
    }

    // =========================
    // LIFECYCLE
    // =========================
    override fun onStart() {
        super.onStart()

        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()

        auth.removeAuthStateListener(authListener)
    }
}