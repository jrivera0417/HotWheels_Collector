package com.example.hotwheelscollector.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.User
import com.example.hotwheelscollector.utils.FavoriteEvents

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var favoriteListener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())
        val defaultEmail = "local@user.com"

        //USER SETUP
        var user = db.getUserByEmail(defaultEmail)

        if (user == null) {
            db.insertUser(
                User(
                    name = "Local User",
                    email = defaultEmail,
                    password = "1234"
                )
            )
            user = db.getUserByEmail(defaultEmail)
        }

        val userId = user?.id ?: return
        var cachedCars = db.getCarsByUser(userId)

        //VIEWS
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val contentView = view.findViewById<View>(R.id.contentView)
        val etBuscar = view.findViewById<EditText>(R.id.etBuscar)

        val btnCollection = view.findViewById<Button>(R.id.btnCollection)

        val imgLatest = view.findViewById<ImageView>(R.id.imgLatest)
        val tvLatestName = view.findViewById<TextView>(R.id.tvLatestName)
        val latestCard = view.findViewById<View>(R.id.latestCarCard)

        val imgFirst = view.findViewById<ImageView>(R.id.imgFirst)
        val tvFirstName = view.findViewById<TextView>(R.id.tvFirstName)
        val firstCard = view.findViewById<View>(R.id.firstCarCard)
        val txtUltimo = view.findViewById<TextView>(R.id.txtUltimo)
        val txtPrimer = view.findViewById<TextView>(R.id.txtPrimer)

        //DECODER
        fun decodeImage(base64: String, imageView: ImageView) {
            if (base64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imageView.setImageResource(R.drawable.ic_hotwheels_logo)
                }
            } else {
                imageView.setImageResource(R.drawable.ic_hotwheels_logo)
            }
        }

        //NAVIGATION
        fun openCarDetail(carId: Int) {
            val bundle = Bundle().apply {
                putInt("CAR_ID", carId)
                putBoolean("FROM_COLLECTION", false)// 🔥 IMPORTANTE
            }

            findNavController().navigate(
                R.id.carDetailFragment,
                bundle
            )
        }

        //LOAD HOME
        fun loadHome() {

            cachedCars = db.getCarsByUser(userId)

            val cars = cachedCars

            val isEmpty = cars.isEmpty()

            emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            contentView.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (isEmpty) return

            firstCard.visibility = View.VISIBLE
            latestCard.visibility = View.VISIBLE
            txtPrimer.visibility = View.VISIBLE
            txtUltimo.visibility = View.VISIBLE

            val firstCar = cars.firstOrNull()
            val latestCar = cars.lastOrNull()

            //PRIMER CARRO
            firstCar?.let { car ->
                tvFirstName.text = car.name
                decodeImage(car.imageUrl, imgFirst)

                firstCard.setOnClickListener {
                    openCarDetail(car.id)
                }
            }

            //ÚLTIMO CARRO
            latestCar?.let { car ->
                tvLatestName.text = car.name
                decodeImage(car.imageUrl, imgLatest)

                latestCard.setOnClickListener {
                    openCarDetail(car.id)
                }
            }
        }

        loadHome()

        etBuscar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                val query = s.toString().trim()

                if (query.isEmpty()) {

                    loadHome()
                    return
                }

                val result = cachedCars.find { car ->

                    car.name.contains(query, true) ||
                            car.modelCode.contains(query, true) ||
                            car.seriesNumber.contains(query, true)
                }

                if (result != null) {

                    emptyState.visibility = View.GONE
                    contentView.visibility = View.VISIBLE

                    //Ocultar primer agregado
                    firstCard.visibility = View.GONE
                    txtPrimer.visibility = View.GONE
                    txtUltimo.visibility = View.GONE

                    //Mostrar resultado búsqueda
                    latestCard.visibility = View.VISIBLE

                    tvLatestName.text = result.name
                    decodeImage(result.imageUrl, imgLatest)

                    latestCard.setOnClickListener {
                        openCarDetail(result.id)
                    }

                } else {

                    firstCard.visibility = View.GONE
                    latestCard.visibility = View.GONE
                    txtPrimer.visibility = View.GONE
                    txtUltimo.visibility = View.GONE

                    contentView.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        etBuscar.setOnEditorActionListener { v, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                val imm = requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager

                imm.hideSoftInputFromWindow(v.windowToken, 0)

                v.clearFocus()

                val query = etBuscar.text.toString().trim()

                val exists = cachedCars.any {

                    it.name.contains(query, true) ||
                            it.modelCode.contains(query, true) ||
                            it.seriesNumber.contains(query, true)
                }

                if (!exists && query.isNotEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "No se encontraron resultados",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                true
            } else {
                false
            }
        }

        //LISTENER FAVORITOS
        favoriteListener = {
            view.post { loadHome() }
        }

        //NAV A COLECCIÓN
        btnCollection.setOnClickListener {
            findNavController().navigate(R.id.nav_collection)
        }
    }

    override fun onResume() {
        super.onResume()
        favoriteListener?.let { FavoriteEvents.addListener(it) }
    }

    override fun onPause() {
        super.onPause()
        favoriteListener?.let { FavoriteEvents.removeListener(it) }
    }
}