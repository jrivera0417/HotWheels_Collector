package com.example.hotwheelscollector.ui

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderDecoration(
    private val adapter: ColeccionAdapter
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {

        val topChild = parent.getChildAt(0) ?: return
        val topPosition = parent.getChildAdapterPosition(topChild)

        if (topPosition == RecyclerView.NO_POSITION) return

        if (adapter.getItemViewType(topPosition) == ColeccionAdapter.TYPE_HEADER) {
            return
        }

        val headerPosition = getHeaderPositionForItem(topPosition)

        if (headerPosition < 0 || headerPosition >= adapter.itemCount) return

        val headerViewHolder = adapter.onCreateViewHolder(parent, ColeccionAdapter.TYPE_HEADER)

        try {
            adapter.onBindViewHolder(headerViewHolder, headerPosition)
        } catch (e: Exception) {
            return
        }

        val header = headerViewHolder.itemView

        fixLayoutSize(parent, header)

        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    private fun getHeaderPositionForItem(itemPosition: Int): Int {
        var position = itemPosition
        while (position >= 0) {
            if (adapter.getItemViewType(position) == ColeccionAdapter.TYPE_HEADER) {
                return position
            }
            position--
        }
        return 0
    }

    private fun fixLayoutSize(parent: RecyclerView, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}