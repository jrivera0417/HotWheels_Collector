package com.example.hotwheelscollector.data.local

import android.content.Context
import com.example.hotwheelscollector.data.models.User
import com.google.firebase.auth.FirebaseAuth

object SessionManager {

    // =========================
    // FIREBASE USER
    // =========================
    fun getFirebaseUser() =
        FirebaseAuth.getInstance().currentUser

    // =========================
    // LOGGED STATE
    // =========================
    fun isLoggedIn(): Boolean {
        return getFirebaseUser() != null
    }

    // =========================
    // LOCAL SQLITE USER
    // =========================
    fun getLocalUser(context: Context): User? {

        val firebaseUser = getFirebaseUser() ?: return null

        val db = DatabaseHelper(context)

        return db.getUserByFirebaseUid(firebaseUser.uid)
    }

    // =========================
    // GUEST USER
    // =========================
    fun getOrCreateGuestUser(context: Context): User {

        return DatabaseHelper(context)
            .ensureGuestUser()
    }

    // =========================
    // CURRENT USER ID
    // =========================
    fun getCurrentUserId(context: Context): Int {

        return getLocalUser(context)?.id
            ?: getOrCreateGuestUser(context).id
    }

    // =========================
    // LOGOUT
    // =========================
    fun logout() {

        FirebaseAuth.getInstance().signOut()
    }
}