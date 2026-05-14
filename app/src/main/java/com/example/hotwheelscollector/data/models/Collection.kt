package com.example.hotwheelscollector.data.models

data class Collection(
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val totalCars: Int,
    val updatedAt: Long = System.currentTimeMillis()
)