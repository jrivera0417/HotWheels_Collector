package com.example.hotwheelscollector.data.local

import android.content.Context
import com.example.hotwheelscollector.data.models.User
import com.google.firebase.auth.FirebaseAuth

object SessionManager {

    // =========================
    // FIREBASE USER (CLOUD)
    // =========================
    fun getFirebaseUser() =
        FirebaseAuth.getInstance().currentUser

    fun isCloudLoggedIn(): Boolean {
        return getFirebaseUser() != null
    }

    // =========================
    // LOCAL SQLITE USER
    // =========================
    fun getLocalUser(context: Context): User? {

        val db = DatabaseHelper(context)

        // 🔴 CAMBIO CLAVE:
        // ya NO dependemos de Firebase obligatoriamente
        return try {
            val firebaseUser = getFirebaseUser()

            if (firebaseUser != null) {
                db.getUserByFirebaseUid(firebaseUser.uid)
            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }

    // =========================
    // GUEST USER (SIEMPRE DISPONIBLE)
    // =========================
    fun getOrCreateGuestUser(context: Context): User {
        return DatabaseHelper(context)
            .ensureGuestUser()
    }

    // =========================
    // CURRENT USER ID (HÍBRIDO REAL)
    // =========================
    fun getCurrentUserId(context: Context): Int {

        val localUser = getLocalUser(context)

        return localUser?.id
            ?: getOrCreateGuestUser(context).id
    }

    // =========================
    // ESTADO HÍBRIDO
    // =========================
    fun isLoggedInCloud(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun isGuestMode(context: Context): Boolean {
        return getLocalUser(context) == null
    }

    // =========================
    // LOGOUT (SOLO CLOUD)
    // =========================
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}