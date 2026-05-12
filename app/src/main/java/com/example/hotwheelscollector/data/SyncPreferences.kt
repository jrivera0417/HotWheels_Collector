package com.example.hotwheelscollector.data

import android.content.Context

object SyncPreferences {

    private const val PREF_NAME =
        "sync_prefs"

    private const val KEY_LAST_SYNC =
        "last_sync"

    fun saveLastSync(
        context: Context
    ) {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putLong(
                KEY_LAST_SYNC,
                System.currentTimeMillis()
            )
            .apply()
    }

    fun getLastSync(
        context: Context
    ): Long {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getLong(
            KEY_LAST_SYNC,
            0L
        )
    }
}