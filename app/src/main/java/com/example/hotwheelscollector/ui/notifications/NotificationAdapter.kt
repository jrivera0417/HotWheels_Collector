package com.example.hotwheelscollector.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.AppNotification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<AppNotification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imgIcon: ImageView =
            view.findViewById(R.id.imgIcon)

        val tvTitle: TextView =
            view.findViewById(R.id.tvTitle)

        val tvMessage: TextView =
            view.findViewById(R.id.tvMessage)

        val tvTime: TextView =
            view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_notification,
                parent,
                false
            )

        return NotificationViewHolder(view)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {

        val notification = notifications[position]

        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message

        val formatter = SimpleDateFormat(
            "dd MMM • HH:mm",
            Locale.getDefault()
        )

        holder.tvTime.text =
            formatter.format(Date(notification.timestamp))
    }
}