package com.example.hotwheelscollector.ui.profile

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.hotwheelscollector.MainActivity
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.cloud.FirestoreManager
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.example.hotwheelscollector.data.local.SyncPreferences
import com.example.hotwheelscollector.data.notifications.AppNotification
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var layoutGuest: LinearLayout
    private lateinit var layoutLogged: LinearLayout

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvSyncStatus: TextView

    private lateinit var tvCars: TextView
    private lateinit var tvFavs: TextView

    private lateinit var imgProfile: ImageView

    private lateinit var btnEditProfile: MaterialButton

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                saveProfileImage(uri)
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        // =========================
        // LAYOUTS
        // =========================
        layoutGuest =
            view.findViewById(R.id.layoutGuest)

        layoutLogged =
            view.findViewById(R.id.layoutLogged)

        // =========================
        // BUTTONS
        // =========================
        val btnLogin =
            view.findViewById<MaterialButton>(
                R.id.btnLogin
            )

        val btnRegister =
            view.findViewById<MaterialButton>(
                R.id.btnRegister
            )

        val btnSync =
            view.findViewById<MaterialButton>(
                R.id.btnSync
            )

        val btnLogout =
            view.findViewById<MaterialButton>(
                R.id.btnLogout
            )

        btnEditProfile =
            view.findViewById(R.id.btnEditProfile)

        // =========================
        // STATS
        // =========================
        tvCars =
            view.findViewById(R.id.tvCarsCount)

        tvFavs =
            view.findViewById(R.id.tvFavCount)

        // =========================
        // USER INFO
        // =========================
        tvUserName =
            view.findViewById(R.id.tvUserName)

        tvUserEmail =
            view.findViewById(R.id.tvUserEmail)

        tvSyncStatus =
            view.findViewById(R.id.tvSyncStatus)

        imgProfile =
            view.findViewById(R.id.imgProfile)

        // =========================
        // PROFILE IMAGE CLICK
        // =========================
        imgProfile.setOnClickListener {

            val firebaseUser =
                FirebaseAuth.getInstance().currentUser
                    ?: return@setOnClickListener

            if (isGoogleUser()) {

                Toast.makeText(
                    requireContext(),
                    "Foto administrada por Google",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val options = arrayOf(
                "Cambiar foto",
                "Eliminar foto"
            )

            AlertDialog.Builder(requireContext())
                .setTitle("Foto de perfil")
                .setItems(options) { _, which ->

                    when (which) {

                        0 -> {
                            imagePickerLauncher.launch("image/*")
                        }

                        1 -> {

                            val db =
                                DatabaseHelper(requireContext())

                            val currentUserId =
                                SessionManager.getCurrentUserId(
                                    requireContext()
                                )

                            db.updateUserProfileImage(
                                currentUserId,
                                ""
                            )

                            imgProfile.setImageResource(
                                R.drawable.ic_profile
                            )

                            Toast.makeText(
                                requireContext(),
                                "Foto eliminada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .show()
        }

        // =========================
        // LOGIN
        // =========================
        btnLogin.setOnClickListener {

            findNavController()
                .navigate(R.id.loginFragment)
        }

        // =========================
        // REGISTER
        // =========================
        btnRegister.setOnClickListener {

            findNavController()
                .navigate(R.id.registerFragment)
        }

        // =========================
        // EDIT PROFILE
        // =========================
        btnEditProfile.setOnClickListener {

            showEditProfileDialog()
        }

        // =========================
        // SYNC
        // =========================
        btnSync.setOnClickListener {

            btnSync.isEnabled = false

            Toast.makeText(
                requireContext(),
                "Sincronizando...",
                Toast.LENGTH_SHORT
            ).show()

            FirestoreManager(requireContext())
                .syncAllToCloud(

                    onSuccess = {

                        requireActivity().runOnUiThread {

                            btnSync.isEnabled = true

                            // =========================
                            // NOTIFICACIÓN LOCAL
                            // =========================
                            val db = DatabaseHelper(requireContext())

                            db.insertNotification(
                                AppNotification(
                                    title = "Sincronización completada",
                                    message = "Tu colección fue sincronizada correctamente con la nube.",
                                    timestamp = System.currentTimeMillis(),
                                    isRead = false
                                )
                            )

                            // =========================
                            // REFRESH DOT
                            // =========================
                            (requireActivity() as MainActivity)
                                .refreshNotificationDot()

                            Toast.makeText(
                                requireContext(),
                                "Colección sincronizada",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },

                    onError = { error ->

                        requireActivity()
                            .runOnUiThread {

                                btnSync.isEnabled = true

                                Toast.makeText(
                                    requireContext(),
                                    error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                )
        }

        // =========================
        // LOGOUT
        // =========================
        btnLogout.setOnClickListener {

            SessionManager.logout()

            requireActivity()
                .findViewById<ImageView>(
                    R.id.imgSyncStatus
                )
                .visibility = View.GONE

            Toast.makeText(
                requireContext(),
                "Sesión cerrada",
                Toast.LENGTH_SHORT
            ).show()

            refreshProfile()
        }

        // =========================
        // INITIAL STATE
        // =========================
        refreshProfile()
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    // =========================
    // REFRESH PROFILE
    // =========================
    private fun refreshProfile() {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser

        val isLoggedIn =
            firebaseUser != null

        updateProfileState(
            isLoggedIn = isLoggedIn,
            userName = firebaseUser?.displayName
                ?: "Usuario",
            userEmail = firebaseUser?.email ?: ""
        )

        refreshStats()

        btnEditProfile.text =
            if (isGoogleUser()) {
                "Editar nombre"
            } else {
                "Editar perfil"
            }

        // =========================
        // LAST SYNC
        // =========================
        val lastSync =
            SyncPreferences.getLastSync(
                requireContext()
            )

        if (lastSync == 0L) {

            tvSyncStatus.text =
                "Última sincronización: Nunca"

        } else {

            val formatter =
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                )

            val formattedDate =
                formatter.format(Date(lastSync))

            tvSyncStatus.text =
                "Última sincronización: $formattedDate"
        }

        // =========================
        // PROFILE IMAGE
        // =========================
        if (firebaseUser != null) {

            if (isGoogleUser()) {

                if (firebaseUser.photoUrl != null) {

                    Glide.with(requireContext())
                        .load(firebaseUser.photoUrl)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(
                            DiskCacheStrategy.NONE
                        )
                        .placeholder(R.drawable.ic_profile)
                        .into(imgProfile)

                } else {

                    imgProfile.setImageResource(
                        R.drawable.ic_profile
                    )
                }

            } else {

                val db =
                    DatabaseHelper(requireContext())

                val currentUserId =
                    SessionManager.getCurrentUserId(
                        requireContext()
                    )

                val base64Image =
                    db.getProfileImage(currentUserId)

                if (!base64Image.isNullOrEmpty()) {

                    val imageBytes =
                        Base64.decode(
                            base64Image,
                            Base64.DEFAULT
                        )

                    Glide.with(this)
                        .load(imageBytes)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.ic_profile)
                        .into(imgProfile)

                } else {

                    imgProfile.setImageResource(
                        R.drawable.ic_profile
                    )
                }
            }

        } else {

            imgProfile.setImageResource(
                R.drawable.ic_profile
            )
        }
    }

    // =========================
    // PROFILE STATE
    // =========================
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

    // =========================
    // REFRESH STATS
    // =========================
    private fun refreshStats() {

        val db =
            DatabaseHelper(requireContext())

        val currentUserId =
            SessionManager.getCurrentUserId(
                requireContext()
            )

        val cars =
            db.getCarsByUser(currentUserId)

        val favorites =
            db.getFavoriteCars(currentUserId)

        tvCars.text =
            cars.size.toString()

        tvFavs.text =
            favorites.size.toString()
    }

    // =========================
    // GOOGLE USER
    // =========================
    private fun isGoogleUser(): Boolean {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return false

        return firebaseUser.providerData.any {

            it.providerId == "google.com"
        }
    }

    // =========================
    // EDIT PROFILE DIALOG
    // =========================
    private fun showEditProfileDialog() {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val editText =
            EditText(requireContext())

        editText.setText(
            firebaseUser.displayName ?: ""
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Editar nombre")

            .setView(editText)

            .setPositiveButton(
                "Guardar"
            ) { _, _ ->

                val newName =
                    editText.text.toString().trim()

                if (newName.isEmpty()) {
                    return@setPositiveButton
                }

                val profileUpdates =
                    UserProfileChangeRequest
                        .Builder()
                        .setDisplayName(newName)
                        .build()

                firebaseUser.updateProfile(
                    profileUpdates
                )

                    .addOnSuccessListener {

                        tvUserName.text = newName

                        Toast.makeText(
                            requireContext(),
                            "Nombre actualizado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    .addOnFailureListener {

                        Toast.makeText(
                            requireContext(),
                            "Error actualizando perfil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            .setNegativeButton(
                "Cancelar",
                null
            )

            .show()
    }

    // =========================
    // SAVE PROFILE IMAGE
    // =========================
    private fun saveProfileImage(
        uri: Uri
    ) {

        try {

            val inputStream: InputStream? =
                requireContext()
                    .contentResolver
                    .openInputStream(uri)

            val bitmap =
                BitmapFactory.decodeStream(
                    inputStream
                )

            val outputStream =
                ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                70,
                outputStream
            )

            val imageBytes =
                outputStream.toByteArray()

            val imageBase64 =
                Base64.encodeToString(
                    imageBytes,
                    Base64.DEFAULT
                )

            val db =
                DatabaseHelper(requireContext())

            val currentUserId =
                SessionManager.getCurrentUserId(
                    requireContext()
                )

            db.saveProfileImage(
                currentUserId,
                imageBase64
            )

            Glide.with(this)
                .load(imageBytes)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile)
                .into(imgProfile)

            Toast.makeText(
                requireContext(),
                "Foto actualizada",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Error cargando imagen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}