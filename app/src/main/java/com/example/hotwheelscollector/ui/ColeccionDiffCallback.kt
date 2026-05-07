package com.example.hotwheelscollector.ui

import androidx.recyclerview.widget.DiffUtil

class ColeccionDiffCallback : DiffUtil.ItemCallback<ColeccionItem>() {

    override fun areItemsTheSame(oldItem: ColeccionItem, newItem: ColeccionItem): Boolean {
        return when {
            oldItem is ColeccionItem.Header && newItem is ColeccionItem.Header ->
                oldItem.collectionId == newItem.collectionId

            oldItem is ColeccionItem.CarItem && newItem is ColeccionItem.CarItem ->
                oldItem.car.id == newItem.car.id

            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ColeccionItem, newItem: ColeccionItem): Boolean {

        return when {

            oldItem is ColeccionItem.Header && newItem is ColeccionItem.Header ->
                oldItem.title == newItem.title &&
                        oldItem.expanded == newItem.expanded

            oldItem is ColeccionItem.CarItem && newItem is ColeccionItem.CarItem ->
                oldItem.car == newItem.car

            else -> false
        }
    }
}