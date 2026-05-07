package com.example.hotwheelscollector.ui

import com.example.hotwheelscollector.data.Car

sealed class ColeccionItem {

    data class Header(
        val title: String,
        val collectionId: Int,
        val expanded: Boolean
    ) : ColeccionItem()

    data class CarItem(val car: Car) : ColeccionItem()
}