package com.example.hotwheelscollector.data

import android.content.Context
import com.example.hotwheelscollector.MainActivity

object NotificationHelper {

    // =========================
    // CREAR NOTIFICACIÓN
    // =========================
    private fun createNotification(
        context: Context,
        title: String,
        message: String
    ) {

        val db = DatabaseHelper(context)

        val notification = AppNotification(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        db.insertNotification(notification)

        // REFRESH DOT
        val activity = context as? MainActivity

        activity?.runOnUiThread {
            activity.refreshNotificationDot()
        }
    }

    // =========================
    // CAR ADDED
    // =========================
    fun carAdded(context: Context, carName: String) {

        createNotification(
            context,
            "Carro añadido",
            "$carName fue agregado a tu colección"
        )
    }

    // =========================
    // CAR DELETED
    // =========================
    fun carDeleted(context: Context, carName: String) {

        createNotification(
            context,
            "Carro eliminado",
            "$carName fue eliminado de tu colección"
        )
    }

    // =========================
    // CAR RESTORED
    // =========================
    fun carRestored(context: Context, carName: String) {

        createNotification(
            context,
            "Carro restaurado",
            "$carName fue restaurado a tu colección"
        )
    }

    // =========================
    // FAVORITE ADDED
    // =========================
    fun favoriteAdded(context: Context, carName: String) {

        createNotification(
            context,
            "Favorito agregado",
            "$carName fue añadido a favoritos"
        )
    }

    // =========================
    // COLLECTION CREATED
    // =========================
    fun collectionCreated(
        context: Context,
        collectionName: String
    ) {

        createNotification(
            context,
            "Colección creada",
            "La colección $collectionName fue creada"
        )
    }

    // =========================
    // APP UPDATE
    // =========================
    fun appUpdated(context: Context) {

        createNotification(
            context,
            "Nueva actualización",
            "Hay novedades disponibles en la app"
        )
    }
}