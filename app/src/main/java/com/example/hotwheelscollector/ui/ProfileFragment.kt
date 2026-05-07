package com.example.hotwheelscollector.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btnRegister)

        val tvCars = view.findViewById<TextView>(R.id.tvCarsCount)
        val tvFavs = view.findViewById<TextView>(R.id.tvFavCount)

        val db = DatabaseHelper(requireContext())

        val currentUserId = 1

        val cars = db.getCarsByUser(currentUserId)
        val favorites = db.getFavoriteCars(currentUserId)

        tvCars.text = cars.size.toString()
        tvFavs.text = favorites.size.toString()

        btnLogin.setOnClickListener {
            Toast.makeText(requireContext(), "Ir a login", Toast.LENGTH_SHORT).show()
        }

        btnRegister.setOnClickListener {
            Toast.makeText(requireContext(), "Ir a registro", Toast.LENGTH_SHORT).show()
        }
    }
}