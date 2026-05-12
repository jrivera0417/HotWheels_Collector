package com.example.hotwheelscollector.ui.auth

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.FirestoreManager
import com.example.hotwheelscollector.data.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // =========================
    // GOOGLE SIGN-IN LAUNCHER
    // =========================
    private val googleLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val task =
                    GoogleSignIn.getSignedInAccountFromIntent(
                        result.data
                    )

                try {

                    val account: GoogleSignInAccount =
                        task.getResult(ApiException::class.java)

                    firebaseAuthWithGoogle(
                        account.idToken!!
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        requireContext(),
                        "Error Google Sign-In",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // =========================
        // GOOGLE CONFIG
        // =========================
        val gso =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(
                    getString(
                        R.string.default_web_client_id
                    )
                )
                .requestEmail()
                .build()

        googleSignInClient =
            GoogleSignIn.getClient(
                requireActivity(),
                gso
            )

        val etEmail =
            view.findViewById<TextInputEditText>(
                R.id.etEmail
            )

        val etPassword =
            view.findViewById<TextInputEditText>(
                R.id.etPassword
            )

        val btnLogin =
            view.findViewById<MaterialButton>(
                R.id.btnDoLogin
            )

        val btnGoogle =
            view.findViewById<SignInButton>(
                R.id.btnGoogleLogin
            )

        val tvGoRegister =
            view.findViewById<TextView>(
                R.id.tvGoRegister
            )

        // =========================
        // LOGIN EMAIL
        // =========================
        btnLogin.setOnClickListener {

            val email =
                etEmail.text.toString().trim()

            val password =
                etPassword.text.toString().trim()

            // VALIDACIONES
            if (email.isEmpty()) {

                etEmail.error =
                    "Ingresa tu correo"

                return@setOnClickListener
            }

            if (
                !Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                etEmail.error =
                    "Correo inválido"

                return@setOnClickListener
            }

            if (password.length < 6) {

                etPassword.error =
                    "Mínimo 6 caracteres"

                return@setOnClickListener
            }

            // FIREBASE LOGIN
            auth.signInWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val firebaseUser =
                        auth.currentUser

                    if (firebaseUser != null) {

                        val db =
                            DatabaseHelper(requireContext())

                        var localUser =
                            db.getUserByFirebaseUid(
                                firebaseUser.uid
                            )

                        // =========================
                        // CREAR USUARIO SQLITE
                        // =========================
                        if (localUser == null) {

                            db.insertUser(
                                User(
                                    name = firebaseUser.displayName
                                        ?: "Usuario",

                                    email = firebaseUser.email
                                        ?: "",

                                    password = "",

                                    firebaseUid =
                                        firebaseUser.uid
                                )
                            )

                            localUser =
                                db.getUserByFirebaseUid(
                                    firebaseUser.uid
                                )
                        }

                        // =========================
                        // MIGRAR DATOS INVITADO
                        // =========================
                        localUser?.let {

                            db.migrateGuestDataToUser(
                                it.id
                            )
                        }
                    }

                    restoreCloudData {

                        Toast.makeText(
                            requireContext(),
                            "Sesión iniciada",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController()
                            .popBackStack()
                    }

                } else {

                    Toast.makeText(
                        requireContext(),
                        task.exception?.message
                            ?: "Error de login",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // =========================
        // GOOGLE LOGIN
        // =========================
        btnGoogle.setOnClickListener {

            googleSignInClient.signOut()

            val signInIntent =
                googleSignInClient.signInIntent

            googleLauncher.launch(signInIntent)
        }

        // =========================
        // REGISTER
        // =========================
        tvGoRegister.setOnClickListener {

            findNavController().navigate(
                R.id.registerFragment
            )
        }
    }

    // =========================
    // FIREBASE GOOGLE AUTH
    // =========================
    private fun firebaseAuthWithGoogle(
        idToken: String
    ) {

        val credential =
            GoogleAuthProvider.getCredential(
                idToken,
                null
            )

        auth.signInWithCredential(
            credential
        ).addOnCompleteListener(
            requireActivity()
        ) { task ->

            if (task.isSuccessful) {

                val firebaseUser =
                    auth.currentUser

                if (firebaseUser != null) {

                    val db =
                        DatabaseHelper(requireContext())

                    var localUser =
                        db.getUserByFirebaseUid(
                            firebaseUser.uid
                        )

                    // =========================
                    // CREAR USUARIO SQLITE
                    // =========================
                    if (localUser == null) {

                        db.insertUser(
                            User(
                                name = firebaseUser.displayName
                                    ?: "Usuario",

                                email = firebaseUser.email
                                    ?: "",

                                password = "",

                                firebaseUid =
                                    firebaseUser.uid
                            )
                        )

                        localUser =
                            db.getUserByFirebaseUid(
                                firebaseUser.uid
                            )
                    }

                    // =========================
                    // MIGRAR DATOS INVITADO
                    // =========================
                    localUser?.let {

                        db.migrateGuestDataToUser(
                            it.id
                        )
                    }
                }

                restoreCloudData {

                    Toast.makeText(
                        requireContext(),
                        "Sesión iniciada",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController()
                        .popBackStack()
                }

            } else {

                Toast.makeText(
                    requireContext(),
                    task.exception?.message
                        ?: "Error de login",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================
    // RESTORE CLOUD DATA
    // =========================
    private fun restoreCloudData(
        onComplete: () -> Unit
    ) {

        FirestoreManager(requireContext())
            .restoreFromCloud(

                onSuccess = {

                    requireActivity().runOnUiThread {

                        onComplete()
                    }
                },

                onError = {

                    requireActivity().runOnUiThread {

                        onComplete()
                    }
                }
            )
    }
}