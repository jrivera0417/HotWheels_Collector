package com.example.hotwheelscollector.ui

import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.Car
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.utils.FavoriteEvents
import com.google.android.material.appbar.*
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

class CarDetailFragment : Fragment(R.layout.fragment_car_detail) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())

        val carId = arguments?.getInt("CAR_ID") ?: return
        val fromCollection = arguments?.getBoolean("FROM_COLLECTION") ?: false
        val car = db.getCarById(carId) ?: return
        val collection = db.getCollectionById(car.collectionId)

        val imgCar = view.findViewById<ImageView>(R.id.imgCar)
        val blurOverlay = view.findViewById<View>(R.id.blurOverlay)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val collapsing = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val appBar = view.findViewById<AppBarLayout>(R.id.appBar)

        val btnFav = view.findViewById<ImageButton>(R.id.btnFavorite)
        val btnShare = view.findViewById<ImageButton>(R.id.btnShare)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvCollection = view.findViewById<TextView>(R.id.tvCollection)
        val tvNumber = view.findViewById<TextView>(R.id.tvNumber)
        val tvColor = view.findViewById<TextView>(R.id.tvColor)
        val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        val tvStore = view.findViewById<TextView>(R.id.tvStore)

        //Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        collapsing.title = car.name

        btnEdit.visibility = if (fromCollection) View.VISIBLE else View.GONE
        btnDelete.visibility = if (fromCollection) View.VISIBLE else View.GONE

        imgCar.transitionName = "car_image_${car.id}"

        val formatter = NumberFormat.getInstance(Locale("es", "CO"))
        val priceFormatted = formatter.format(car.price)

        tvName.text = car.name
        tvCollection.text = collection?.name ?: "Sin colección"
        tvNumber.text = "${car.collectionNumber} / ${collection?.totalCars ?: "-"}"
        tvColor.text = car.color
        tvPrice.text = "$$priceFormatted"
        tvStore.text = "Comprado en: ${car.store}"

        //Click imagen
        imgCar.setOnClickListener {

            val intent = android.content.Intent(requireContext(), FullImageActivity::class.java).apply {
                putExtra("IMAGE", car.imageUrl)
            }
            startActivity(intent)
        }

        //Imagen + palette
        if (car.imageUrl.isNotEmpty()) {
            try {
                val bytes = Base64.decode(car.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                if (bitmap != null) {
                    imgCar.setImageBitmap(bitmap)
                    applyPaletteFromBitmap(bitmap, collapsing, blurOverlay)
                }

            } catch (e: Exception) {
                imgCar.setImageResource(R.drawable.ic_hotwheels_logo)
            }
        }

        //Blur real
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurOverlay.doOnPreDraw {
                blurOverlay.setRenderEffect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                )
            }
        }

        //Scroll
        appBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val progress = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
                blurOverlay.alpha = progress

                imgCar.scaleX = 1f + (0.2f * (1 - progress))
                imgCar.scaleY = 1f + (0.2f * (1 - progress))
            }
        )

        //Favorito
        var isFavorite = car.favorite

        fun updateFav() {
            btnFav.setImageResource(
                if (isFavorite) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
        }

        updateFav()

        btnFav.setOnClickListener {
            isFavorite = !isFavorite
            db.updateFavorite(car.id, isFavorite)
            updateFav()
            FavoriteEvents.notifyChange()
        }

        //Edit
        btnEdit.setOnClickListener {

            val bundle = Bundle().apply {
                putInt("CAR_ID", car.id)
            }

            findNavController().navigate(
                R.id.action_carDetail_to_editCar,
                bundle
            )
        }

        //Share
        btnShare.setOnClickListener {
            shareCollectorCard(car, collection?.name ?: "", priceFormatted)
        }

        //Delete
        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar auto")
                .setMessage("¿Seguro que quieres eliminar este carro?")
                .setPositiveButton("Eliminar") { _, _ ->

                    val deletedCar = car
                    db.deleteCar(car.id)
                    FavoriteEvents.notifyChange()

                    findNavController().popBackStack()

                    view.post {
                        if (isAdded) {
                            Snackbar.make(requireView(), "Auto eliminado", Snackbar.LENGTH_LONG)
                                .setAction("DESHACER") {
                                    db.insertCar(deletedCar)
                                    FavoriteEvents.notifyChange()
                                }
                                .show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    //PALETTE
    private fun applyPaletteFromBitmap(bitmap: Bitmap, collapsing: CollapsingToolbarLayout, overlay: View) {
        Palette.from(bitmap).generate { palette ->

            val dominant = palette?.getDominantColor(Color.parseColor("#1A1A1A"))
            val vibrant = palette?.getVibrantColor(dominant!!) ?: dominant!!

            val gradient = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(lightenColor(vibrant), vibrant, darkenColor(vibrant))
            )

            collapsing.setContentScrimColor(darkenColor(vibrant))
            overlay.background = gradient
        }
    }

    //COLOR HELPERS
    private fun lightenColor(color: Int): Int {
        return Color.rgb(
            (Color.red(color) + 60).coerceAtMost(255),
            (Color.green(color) + 60).coerceAtMost(255),
            (Color.blue(color) + 60).coerceAtMost(255)
        )
    }

    private fun darkenColor(color: Int): Int {
        return Color.rgb(
            (Color.red(color) * 0.6f).toInt(),
            (Color.green(color) * 0.6f).toInt(),
            (Color.blue(color) * 0.6f).toInt()
        )
    }

    private fun getColorFromName(colorName: String): Int {
        val clean = colorName.trim().lowercase()

        return try {
            when {
                clean.startsWith("#") -> Color.parseColor(clean)
                clean.contains("rojo") -> Color.parseColor("#E53935")
                clean.contains("azul") -> Color.parseColor("#1E88E5")
                clean.contains("verde") -> Color.parseColor("#43A047")
                clean.contains("amarillo") -> Color.parseColor("#FDD835")
                clean.contains("negro") -> Color.parseColor("#212121")
                clean.contains("blanco") -> Color.parseColor("#ECEFF1")
                clean.contains("gris") -> Color.parseColor("#757575")
                clean.contains("naranja") -> Color.parseColor("#FB8C00")
                clean.contains("morado") || clean.contains("violeta") -> Color.parseColor("#8E24AA")
                else -> Color.parseColor("#1A1A1A")
            }
        } catch (e: Exception) {
            Color.parseColor("#1A1A1A")
        }
    }

    //SHARE
    private fun shareCollectorCard(car: Car, collectionName: String, price: String) {

        val width = 1080
        val height = 1600

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val baseColor = getColorFromName(car.color)
        val dark = darkenColor(baseColor)

        // =========================
        // FONDO PROFUNDO
        // =========================
        val bg = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.BLACK, dark),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = bg
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // =========================
        // CARD CENTRADA
        // =========================
        val cardRect = RectF(
            100f,
            150f,
            width - 100f,
            height - 150f
        )

        // =========================
        // BORDE HOLOGRÁFICO
        // =========================
        val holoColors = intArrayOf(
            Color.RED,
            Color.MAGENTA,
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.YELLOW,
            Color.RED
        )

        val holo = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            holoColors,
            null,
            Shader.TileMode.MIRROR
        )

        paint.shader = holo
        canvas.drawRoundRect(
            cardRect.left - 6,
            cardRect.top - 6,
            cardRect.right + 6,
            cardRect.bottom + 6,
            50f,
            50f,
            paint
        )
        paint.shader = null

        // =========================
        // BASE CARD
        // =========================
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRoundRect(cardRect, 45f, 45f, paint)

        // =========================
        // 💡 GLOW
        // =========================
        paint.setShadowLayer(60f, 0f, 0f, baseColor)
        canvas.drawRoundRect(cardRect, 45f, 45f, paint)
        paint.clearShadowLayer()

        // =========================
        // IMAGEN
        // =========================
        if (car.imageUrl.isNotEmpty()) {
            try {
                val bytes = Base64.decode(car.imageUrl, Base64.DEFAULT)
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                if (original != null) {

                    val imgRect = RectF(
                        cardRect.left,
                        cardRect.top,
                        cardRect.right,
                        cardRect.top + 600f
                    )

                    val path = Path()
                    path.addRoundRect(imgRect, 45f, 45f, Path.Direction.CW)

                    canvas.save()
                    canvas.clipPath(path)

                    val scaled = Bitmap.createScaledBitmap(
                        original,
                        imgRect.width().toInt(),
                        imgRect.height().toInt(),
                        true
                    )

                    canvas.drawBitmap(scaled, imgRect.left, imgRect.top, null)
                    canvas.restore()
                }

            } catch (_: Exception) {}
        }

        // =========================
        // OVERLAY OSCURO
        // =========================
        val overlay = LinearGradient(
            0f,
            cardRect.top + 400f,
            0f,
            cardRect.bottom,
            Color.TRANSPARENT,
            dark,
            Shader.TileMode.CLAMP
        )

        paint.shader = overlay
        canvas.drawRect(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.bottom,
            paint
        )
        paint.shader = null

        // =========================
        // TEXTO CENTRADO
        // =========================
        val title = Paint().apply {
            color = Color.WHITE
            textSize = 58f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val normal = Paint().apply {
            color = Color.LTGRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val centerX = cardRect.centerX()
        var y = cardRect.top + 700f

        canvas.drawText("🚗 ${car.name}", centerX, y, title); y += 90
        canvas.drawText("📦 $collectionName", centerX, y, normal); y += 65
        canvas.drawText("🔢 #${car.collectionNumber}", centerX, y, normal); y += 65
        canvas.drawText("🎨 ${car.color}", centerX, y, normal); y += 65
        canvas.drawText("💰 $$price", centerX, y, normal); y += 65
        canvas.drawText("🏪 ${car.store}", centerX, y, normal)

        // =========================
        // FOIL DINÁMICO
        // =========================
        val foil = LinearGradient(
            -width.toFloat(),
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                Color.TRANSPARENT,
                Color.WHITE,
                Color.TRANSPARENT
            ),
            floatArrayOf(0.3f, 0.5f, 0.7f),
            Shader.TileMode.CLAMP
        )

        paint.shader = foil
        paint.alpha = 90

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        paint.alpha = 255

        // =========================
        // BRAND
        // =========================
        val brand = Paint().apply {
            color = Color.GRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "COLLECTOR'S EDITION • Hot Wheels Collector",
            centerX,
            cardRect.bottom - 40f,
            brand
        )

        // =========================
        // SHARE
        // =========================
        try {
            val file = File(requireContext().cacheDir, "ultra_card.png")
            val stream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Compartir carta ULTRA"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}