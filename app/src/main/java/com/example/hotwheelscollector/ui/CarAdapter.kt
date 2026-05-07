package com.example.hotwheelscollector.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.Car
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.utils.FavoriteEvents

class CarAdapter(
    private val list: MutableList<Car>,
    private val db: DatabaseHelper,
    private val onListEmpty: () -> Unit,
    private val onCarClick: (Car) -> Unit
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val image: ImageView = view.findViewById(R.id.imgCar)
        val favorite: ImageView = view.findViewById(R.id.imgFavorite)
        val number: TextView = view.findViewById(R.id.tvNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {

        val car = list[position]

        holder.name.text = car.name
        holder.number.text = car.collectionNumber

        //Imagen
        if (car.imageUrl.isNotEmpty()) {
            try {
                val bytes = Base64.decode(car.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.image.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.image.setImageResource(R.drawable.ic_hotwheels_logo)
            }
        } else {
            holder.image.setImageResource(R.drawable.ic_hotwheels_logo)
        }

        //Estado favorito
        holder.favorite.setImageResource(
            if (car.favorite)
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )

        //Toggle favorito
        holder.favorite.setOnClickListener {

            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val currentCar = list[pos]
            val newState = !currentCar.favorite

            db.updateFavorite(currentCar.id, newState)

            list[pos] = currentCar.copy(favorite = newState)
            notifyItemChanged(pos)

            FavoriteEvents.notifyChange()

            //Si se queda sin items
            if (list.isEmpty()) {
                onListEmpty()
            }
        }

        //CLICK → DETALLE
        holder.itemView.setOnClickListener {
            onCarClick(car)
        }
    }
    override fun getItemCount(): Int = list.size
}