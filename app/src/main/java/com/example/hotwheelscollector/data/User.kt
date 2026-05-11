package com.example.hotwheelscollector.data

data class User(
    val id: Int = 0,
    val name: String,
    val email: String,
    val password: String,
    val firebaseUid: String = ""
)