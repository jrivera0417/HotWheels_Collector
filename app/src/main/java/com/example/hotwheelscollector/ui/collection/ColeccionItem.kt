package com.example.hotwheelscollector.ui.collection

import com.example.hotwheelscollector.data.models.Car

sealed class ColeccionItem {

    data class Header(
        val title: String,
        val collectionId: Int,
        val expanded: Boolean
    ) : ColeccionItem()

    data class CarItem(val car: Car) : ColeccionItem()
}