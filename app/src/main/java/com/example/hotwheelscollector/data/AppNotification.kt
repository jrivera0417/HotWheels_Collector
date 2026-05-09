package com.example.hotwheelscollector.data

data class AppNotification(

    val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean
)