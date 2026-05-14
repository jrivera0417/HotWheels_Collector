package com.example.hotwheelscollector.ui.collection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.utils.FavoriteEvents
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File

class EditCarFragment : Fragment(R.layout.fragment_edit_car) {

    private lateinit var imgPreview: ImageView
    private var selectedBitmap: Bitmap? = null

    // =========================
    // GALERÍA
    // =========================
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        uri?.let {
            startCrop(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())

        val carId = arguments?.getInt("CAR_ID") ?: return
        val car = db.getCarById(carId) ?: return

        val etName = view.findViewById<EditText>(R.id.etName)
        val etColor = view.findViewById<EditText>(R.id.etColor)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)
        val etStore = view.findViewById<EditText>(R.id.etStore)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        imgPreview = view.findViewById(R.id.imgPreview)

        // =========================
        // CARGAR DATOS
        // =========================
        etName.setText(car.name)
        etColor.setText(car.color)
        etPrice.setText(car.price.toString())
        etStore.setText(car.store)

        if (car.imageUrl.isNotEmpty()) {
            try {
                val bytes = Base64.decode(car.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgPreview.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }

        // =========================
        // CLICK IMAGEN
        // =========================
        imgPreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // =========================
        // GUARDAR
        // =========================
        btnSave.setOnClickListener {

            val imageBase64 = selectedBitmap?.let { bitmapToBase64(it) } ?: car.imageUrl

            val updatedCar = car.copy(
                name = etName.text.toString(),
                color = etColor.text.toString(),
                price = etPrice.text.toString().toDoubleOrNull() ?: 0.0,
                store = etStore.text.toString(),
                imageUrl = imageBase64
            )

            db.updateCar(updatedCar)

            FavoriteEvents.notifyChange()

            Toast.makeText(requireContext(), "Auto actualizado 🚀", Toast.LENGTH_SHORT).show()

            findNavController().popBackStack()
        }
    }

    // =========================
    // START CROP
    // =========================
    private fun startCrop(sourceUri: Uri) {

        val destinationUri = Uri.fromFile(
            File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setCompressionQuality(90)

            setToolbarTitle("Recortar imagen")
            setToolbarColor(Color.WHITE)
            setStatusBarColor(Color.WHITE)
            setActiveControlsWidgetColor(Color.RED)

            setFreeStyleCropEnabled(true)
            withAspectRatio(1f, 1f) // estilo Instagram cuadrado
        }

        UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
    }

    // =========================
    // RESULTADO DEL CROP
    // =========================
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {

            val resultUri = UCrop.getOutput(data!!)

            if (resultUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().contentResolver,
                    resultUri
                )

                selectedBitmap = bitmap
                imgPreview.setImageBitmap(bitmap)
            }
        }

        if (resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(data!!)
            error?.printStackTrace()
        }
    }

    // =========================
    // BITMAP → BASE64
    // =========================
    private fun bitmapToBase64(bitmap: Bitmap): String {

        //Reducir tamaño
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            800, // ancho máximo
            (bitmap.height * (800.0 / bitmap.width)).toInt(),
            true
        )

        val stream = ByteArrayOutputStream()

        //COMPRESIÓN
        resized.compress(Bitmap.CompressFormat.JPEG, 75, stream)

        val bytes = stream.toByteArray()

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}