package com.example.hotwheelscollector.ui.collection

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.notifications.NotificationHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class AddCarBottomSheet(
    private val onCarAdded: () -> Unit
) : BottomSheetDialogFragment(R.layout.fragment_add_car) {

    private var imageData: String = ""
    private var selectedCollectionId: Int = -1

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            startCrop(uri)
        }
    }

    // =========================
    // UCROP
    // =========================
    private fun startCrop(uri: Uri) {

        val destFile = File(requireContext().cacheDir, "crop_${UUID.randomUUID()}.jpg")
        val destUri = Uri.fromFile(destFile)

        UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .withOptions(getCropOptions())
            .start(requireContext(), this)
    }

    private fun getCropOptions(): UCrop.Options {
        val options = UCrop.Options()
        options.setToolbarTitle("Recortar imagen")
        options.setToolbarColor(Color.WHITE)
        options.setStatusBarColor(Color.WHITE)
        options.setToolbarWidgetColor(Color.BLACK)
        options.setFreeStyleCropEnabled(true)
        return options
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {

            val resultUri = UCrop.getOutput(data!!) ?: return

            val bitmap = requireContext().contentResolver.openInputStream(resultUri)
                ?.use { BitmapFactory.decodeStream(it) }

            if (bitmap != null) {
                view?.findViewById<ImageView>(R.id.imgPreview)
                    ?.setImageBitmap(bitmap)

                imageData = bitmapToBase64(bitmap)
            }
        }
    }

    // =========================
    // VIEW
    // =========================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())
        val userId = SessionManager.getCurrentUserId(requireContext())

        val etName = view.findViewById<EditText>(R.id.etName)
        val etBrand = view.findViewById<EditText>(R.id.etBrand)
        val etModelCode = view.findViewById<EditText>(R.id.etModelCode)
        val etCollectionNumber = view.findViewById<EditText>(R.id.etCollectionNumber)
        val etColor = view.findViewById<EditText>(R.id.etColor)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)
        val etStore = view.findViewById<EditText>(R.id.etStore)
        val etQuantity = view.findViewById<EditText>(R.id.etQuantity)

        val etNewCollection = view.findViewById<EditText>(R.id.etNewCollection)
        val etTotalCars = view.findViewById<EditText>(R.id.etTotalCars)
        val spinnerCollections = view.findViewById<Spinner>(R.id.spinnerCollections)

        val rbNew = view.findViewById<RadioButton>(R.id.rbNew)
        val rbExisting = view.findViewById<RadioButton>(R.id.rbExisting)
        val rgCollectionMode = view.findViewById<RadioGroup>(R.id.rgCollectionMode)

        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val imgPreview = view.findViewById<ImageView>(R.id.imgPreview)

        // =========================
        // IMAGEN
        // =========================
        imgPreview.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImage.launch(intent)
        }

        // =========================
        // SPINNER
        // =========================
        val collections = db.getCollectionsByUser(userId)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            collections.map { it.name }
        )

        spinnerCollections.adapter = adapter

        spinnerCollections.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    selectedCollectionId = collections[pos].id
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }

        // =========================
        // RADIOGROUP FIX
        // =========================
        rbNew.isChecked = true

        // Estado inicial
        etNewCollection.visibility = View.VISIBLE
        etTotalCars.visibility = View.VISIBLE
        spinnerCollections.visibility = View.GONE

        rgCollectionMode.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId == R.id.rbNew) {

                etNewCollection.visibility = View.VISIBLE
                etTotalCars.visibility = View.VISIBLE
                spinnerCollections.visibility = View.GONE

            } else {

                etNewCollection.visibility = View.GONE
                etTotalCars.visibility = View.GONE
                spinnerCollections.visibility = View.VISIBLE
            }
        }

        // =========================
        // SAVE
        // =========================
        btnSave.setOnClickListener {

            val name = etName.text.toString().trim()
            val brand = etBrand.text.toString().trim()
            val modelCode = etModelCode.text.toString().trim()
            val collectionNumber = etCollectionNumber.text.toString().trim()
            val color = etColor.text.toString().trim()
            val store = etStore.text.toString().trim()
            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || brand.isEmpty() || modelCode.isEmpty()) {
                Toast.makeText(requireContext(), "Completa campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageData.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona imagen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showCardPreview(
                name, brand, modelCode,
                collectionNumber, color,
                price, store, imageData
            ) {

                var collectionId = selectedCollectionId

                if (rgCollectionMode.checkedRadioButtonId == R.id.rbNew) {

                    val newName = etNewCollection.text.toString().trim()
                    val total = etTotalCars.text.toString().toIntOrNull() ?: 0

                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Nombre de colección requerido", Toast.LENGTH_SHORT).show()
                        return@showCardPreview
                    }

                    collectionId = db.insertCollection(userId, newName, total).toInt()
                }

                val car = Car(
                    userId = userId,
                    collectionId = collectionId,
                    modelCode = modelCode,
                    name = name,
                    brand = brand,
                    collectionNumber = collectionNumber,
                    color = color,
                    price = price,
                    store = store,
                    imageUrl = imageData
                )

                val result = db.insertCar(car)

                if (result != -1L) {

                    NotificationHelper.carAdded(
                        requireContext(),
                        car.name
                    )
                }

                Toast.makeText(requireContext(), "Carro agregado correctamente", Toast.LENGTH_SHORT).show()

                onCarAdded()
                dismiss()
            }
        }
    }

    // =========================
    // BASE64
    // =========================
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    }

    // =========================
    // PREVIEW
    // =========================
    private fun showCardPreview(
        name: String,
        brand: String,
        modelCode: String,
        collectionNumber: String,
        color: String,
        price: Double,
        store: String,
        imageBase64: String,
        onConfirm: () -> Unit
    ) {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_car_preview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val img = dialog.findViewById<ImageView>(R.id.imgPreviewCard)
        val tvName = dialog.findViewById<TextView>(R.id.tvName)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvInfo)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        tvName.text = "🔥 $name"

        tvInfo.text = """
            🚗 $brand
            🔢 #$collectionNumber
            🎨 $color
            💰 $price
            🏪 $store
        """.trimIndent()

        try {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            img.setImageBitmap(bitmap)
        } catch (_: Exception) {}

        btnConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}