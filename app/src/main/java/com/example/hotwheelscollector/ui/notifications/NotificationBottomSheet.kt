package com.example.hotwheelscollector.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationBottomSheet :
    BottomSheetDialogFragment(R.layout.bottomsheet_notifications) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())

        val recycler =
            view.findViewById<RecyclerView>(R.id.recyclerNotifications)

        val layoutEmpty =
            view.findViewById<LinearLayout>(R.id.layoutEmpty)

        val tvClear =
            view.findViewById<TextView>(R.id.tvClear)

        // =========================
        // GET NOTIFICATIONS
        // =========================
        val notifications =
            db.getAllNotifications()

        // =========================
        // EMPTY STATE
        // =========================
        if (notifications.isEmpty()) {

            layoutEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE

        } else {

            layoutEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }

        // =========================
        // RECYCLER
        // =========================
        recycler.layoutManager =
            LinearLayoutManager(requireContext())

        recycler.adapter =
            NotificationAdapter(notifications)

        // =========================
        // MARK READ
        // =========================
        db.markAllNotificationsAsRead()

        // =========================
        // CLEAR ALL
        // =========================
        tvClear.setOnClickListener {

            db.deleteAllNotifications()

            dismiss()
        }
    }
}