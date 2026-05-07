package com.example.hotwheelscollector.data

data class Car(
    val id: Int = 0,
    val userId: Int,
    val collectionId: Int,
    val modelCode: String,
    val name: String,
    val brand: String,
    val seriesNumber: String = "",
    val collectionNumber: String = "",
    val color: String = "",
    val vehicleType: String = "",
    val purchaseDate: String = "",
    val price: Double = 0.0,
    val store: String = "",
    val quantity: Int = 1,
    val favorite: Boolean = false,
    val imageUrl: String = ""
)