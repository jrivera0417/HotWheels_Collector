package com.example.hotwheelscollector.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.models.User
import com.example.hotwheelscollector.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val db = DatabaseHelper(requireContext())

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)

        val btnRegister = view.findViewById<MaterialButton>(R.id.btnDoRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        // =========================
        // REGISTER
        // =========================
        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validaciones
            if (name.isEmpty()) {
                etName.error = "Ingresa tu nombre"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Correo inválido"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            // Firebase Register
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val firebaseUser = auth.currentUser

                        // =========================
                        // Guardar nombre Firebase
                        // =========================
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        firebaseUser?.updateProfile(profileUpdates)

                        // =========================
                        // Guardar usuario SQLite
                        // =========================
                        val localUser = User(
                            name = name,
                            email = email,
                            password = password,
                            firebaseUid = firebaseUser?.uid ?: ""
                        )

                        db.insertUser(localUser)

                        Toast.makeText(
                            requireContext(),
                            "Cuenta creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    } else {

                        Toast.makeText(
                            requireContext(),
                            task.exception?.message ?: "Error al registrarse",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // =========================
        // GO LOGIN
        // =========================
        tvGoLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}