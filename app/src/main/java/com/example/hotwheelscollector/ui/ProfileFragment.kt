package com.example.hotwheelscollector.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var layoutGuest: LinearLayout
    private lateinit var layoutLogged: LinearLayout
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var imgProfile: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =========================
        // Layouts
        // =========================
        layoutGuest = view.findViewById(R.id.layoutGuest)
        layoutLogged = view.findViewById(R.id.layoutLogged)

        // =========================
        // Guest Views
        // =========================
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btnRegister)

        val tvCars = view.findViewById<TextView>(R.id.tvCarsCount)
        val tvFavs = view.findViewById<TextView>(R.id.tvFavCount)

        val btnSync = view.findViewById<MaterialButton>(R.id.btnSync)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        // =========================
        // USER INFO
        // =========================
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)

        imgProfile = view.findViewById(R.id.imgProfile)

        // =========================
        // TEMP SESSION STATE
        // =========================
        refreshProfile()

        // =========================
        // Local Data
        // =========================
        val db = DatabaseHelper(requireContext())

        val currentUserId =
            SessionManager.getCurrentUserId(requireContext())

        val cars = db.getCarsByUser(currentUserId)
        val favorites = db.getFavoriteCars(currentUserId)

        tvCars.text = cars.size.toString()
        tvFavs.text = favorites.size.toString()

        // =========================
        // ACTIONS
        // =========================
        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        btnSync.setOnClickListener {
            Toast.makeText(requireContext(), "Sync en desarrollo", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {

            SessionManager.logout()

            Toast.makeText(
                requireContext(),
                "Sesión cerrada",
                Toast.LENGTH_SHORT
            ).show()

            refreshProfile()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    private fun updateProfileState(
        isLoggedIn: Boolean,
        userName: String = "",
        userEmail: String = ""
    ) {

        layoutGuest.isVisible = !isLoggedIn
        layoutLogged.isVisible = isLoggedIn

        if (isLoggedIn) {

            tvUserName.text = userName
            tvUserEmail.text = userEmail
        }
    }

    private fun refreshProfile() {

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        val isLoggedIn = firebaseUser != null

        updateProfileState(
            isLoggedIn = isLoggedIn,
            userName = firebaseUser?.displayName ?: "Usuario",
            userEmail = firebaseUser?.email ?: ""
        )

        // =========================
        // FOTO PERFIL
        // =========================
        if (firebaseUser?.photoUrl != null) {

            Glide.with(this)
                .load(firebaseUser.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(imgProfile)

        } else {

            imgProfile.setImageResource(R.drawable.ic_profile)
        }
    }
}