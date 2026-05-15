package com.example.hotwheelscollector.data.local

import android.content.Context
import com.example.hotwheelscollector.data.models.User
import com.google.firebase.auth.FirebaseAuth

object SessionManager {

    // =========================
    // FIREBASE USER (SAFE)
    // =========================
    fun getFirebaseUser() =
        FirebaseAuth.getInstance().currentUser

    // =========================
    // LOGGED STATE (MEJORADO)
    // =========================
    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    // =========================
    // LOCAL USER (FIREBASE LINKED)
    // =========================
    fun getLocalUser(context: Context): User? {

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser == null) return null

        val db = DatabaseHelper(context)

        return db.getUserByFirebaseUid(firebaseUser.uid)
    }

    // =========================
    // GUEST USER (SIEMPRE DISPONIBLE)
    // =========================
    fun getOrCreateGuestUser(context: Context): User {

        val db = DatabaseHelper(context)
        val existing = db.getUserByFirebaseUid("")

        return if (existing != null) {
            existing
        } else {
            db.insertUser(
                User(
                    name = "Invitado",
                    email = "guest@local",
                    password = "",
                    firebaseUid = ""
                )
            )

            db.getUserByFirebaseUid("")!!
        }
    }

    // =========================
    // CURRENT USER ID (HÍBRIDO REAL)
    // =========================
    fun getCurrentUserId(context: Context): Int {

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        return if (firebaseUser != null) {

            val db = DatabaseHelper(context)
            val localUser = db.getUserByFirebaseUid(firebaseUser.uid)

            localUser?.id ?: getOrCreateGuestUser(context).id

        } else {

            getOrCreateGuestUser(context).id
        }
    }

    // =========================
    // LOGOUT (CLEAN)
    // =========================
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}