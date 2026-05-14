package com.example.hotwheelscollector.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.models.Collection
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private var categories: List<Collection>,
    private val onClick: (Collection?) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    //CONTROLADO DESDE AFUERA
    private var selectedId: Int? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tv = view.findViewById<TextView>(R.id.tvCategory)
        val card = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size + 1 // "Todos"

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val isAll = position == 0
        val category = if (!isAll) categories[position - 1] else null

        val isSelected = if (isAll) {
            selectedId == null
        } else {
            category?.id == selectedId
        }

        val name = if (isAll) "Todos" else category?.name ?: ""
        holder.tv.text = name

        holder.card.setCardBackgroundColor(
            if (isSelected)
                holder.itemView.context.getColor(R.color.primary)
            else
                holder.itemView.context.getColor(R.color.card_background)
        )

        holder.tv.setTextColor(
            if (isSelected)
                holder.itemView.context.getColor(android.R.color.white)
            else
                holder.itemView.context.getColor(R.color.text_primary)
        )

        holder.card.strokeWidth = if (isSelected) 0 else 1

        holder.itemView.setOnClickListener {

            //Animación
            holder.itemView.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(120)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    holder.itemView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()

            onClick(category)
        }
    }

    //Sincronizar selección desde Fragment
    fun setSelectedCategory(categoryId: Int?) {
        selectedId = categoryId
        notifyDataSetChanged()
    }

    //DATA
    fun updateData(newList: List<Collection>) {
        categories = newList
        notifyDataSetChanged()
    }
}