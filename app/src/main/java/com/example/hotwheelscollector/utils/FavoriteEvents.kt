package com.example.hotwheelscollector.utils

object FavoriteEvents {

    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyChange() {
        listeners.forEach { it.invoke() }
    }
}