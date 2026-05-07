package com.example.hotwheelscollector.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
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

        //VIEWS
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val contentView = view.findViewById<View>(R.id.contentView)

        val btnCollection = view.findViewById<Button>(R.id.btnCollection)

        val imgLatest = view.findViewById<ImageView>(R.id.imgLatest)
        val tvLatestName = view.findViewById<TextView>(R.id.tvLatestName)
        val latestCard = view.findViewById<View>(R.id.latestCarCard)

        val imgFirst = view.findViewById<ImageView>(R.id.imgFirst)
        val tvFirstName = view.findViewById<TextView>(R.id.tvFirstName)
        val firstCard = view.findViewById<View>(R.id.firstCarCard)

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

            val cars = db.getCarsByUser(userId)

            val isEmpty = cars.isEmpty()

            emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            contentView.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (isEmpty) return

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