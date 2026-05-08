package com.example.hotwheelscollector.ui

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.Car
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.databinding.ItemCarBinding
import com.example.hotwheelscollector.databinding.ItemHeaderBinding

class ColeccionAdapter(
    val db: DatabaseHelper,
    val onHeaderClick: (Int) -> Unit,
    val onCarClick: (Car) -> Unit,
    val onChange: () -> Unit
) : ListAdapter<ColeccionItem, RecyclerView.ViewHolder>(ColeccionDiffCallback()) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_CAR = 1
    }

    var currentQuery: String = ""

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ColeccionItem.Header -> TYPE_HEADER
            is ColeccionItem.CarItem -> TYPE_CAR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_HEADER) {

            val binding = ItemHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)

        } else {

            val binding = ItemCarBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            CarViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = getItem(position)) {

            is ColeccionItem.Header -> (holder as HeaderViewHolder).bind(item)

            is ColeccionItem.CarItem -> (holder as CarViewHolder).bind(item.car)
        }
    }

    // =========================
    // HEADER
    // =========================
    inner class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ColeccionItem.Header) {

            binding.tvHeader.text = item.title

            val isExpanded = item.expanded

            binding.headerContainer.isSelected = isExpanded
            binding.headerContainer.elevation = if (isExpanded) 8f else 2f

            binding.imgArrow.animate().cancel()
            binding.imgArrow.rotation = if (isExpanded) 180f else 0f

            binding.tvHeader.alpha = if (isExpanded) 1f else 0.7f

            binding.headerContainer.setOnClickListener {

                binding.headerContainer.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction {

                        binding.headerContainer.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start()

                        onHeaderClick(item.collectionId)
                    }
                    .start()
            }
        }
    }

    // =========================
    // CAR
    // =========================
    inner class CarViewHolder(
        private val binding: ItemCarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {

            //Resetear estado siempre
            binding.root.alpha = 1f
            binding.root.translationY = 0f

            binding.tvName.text = highlightText(car.name)
            binding.tvNumber.text = car.collectionNumber

            //Imagen
            if (car.imageUrl.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(car.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imgCar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.imgCar.setImageResource(R.drawable.ic_hotwheels_logo)
                }
            } else {
                binding.imgCar.setImageResource(R.drawable.ic_hotwheels_logo)
            }

            //Favorito
            binding.imgFavorite.setImageResource(
                if (car.favorite)
                    R.drawable.ic_heart_filled
                else
                    R.drawable.ic_heart_outline
            )

            binding.imgFavorite.setOnClickListener {

                val newState = !car.favorite

                //Desactiva clicks rápidos
                binding.imgFavorite.isEnabled = false

                //Cambio visual inmediato
                binding.imgFavorite.setImageResource(
                    if (newState)
                        R.drawable.ic_heart_filled
                    else
                        R.drawable.ic_heart_outline
                )

                db.updateFavorite(car.id, newState)

                onChange()

                //Reactivar después
                binding.imgFavorite.postDelayed({
                    binding.imgFavorite.isEnabled = true
                }, 150)
            }

            //Click en carro
            binding.root.setOnClickListener {
                onCarClick(car)
            }
        }
    }

    private fun highlightText(text: String): SpannableString {

        val spannable = SpannableString(text)

        if (currentQuery.isBlank()) {
            return spannable
        }

        val startIndex =
            text.lowercase().indexOf(currentQuery.lowercase())

        if (startIndex == -1) {
            return spannable
        }

        val endIndex = startIndex + currentQuery.length

        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#E53935")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }

}