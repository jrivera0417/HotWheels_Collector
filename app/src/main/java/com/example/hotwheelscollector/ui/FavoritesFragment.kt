package com.example.hotwheelscollector.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.User
import com.example.hotwheelscollector.utils.FavoriteEvents

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private lateinit var favoriteListener: () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseHelper(requireContext())
        val defaultEmail = "local@user.com"

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

        val userId = user!!.id

        val btnAddFav = view.findViewById<Button>(R.id.btnAddFav)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerFavorites)

        fun openCarDetail(carId: Int) {
            val bundle = Bundle().apply {
                putInt("CAR_ID", carId)
                putBoolean("FROM_COLLECTION", false) // 🔥 IMPORTANTE
            }

            findNavController().navigate(
                R.id.carDetailFragment,
                bundle
            )
        }

        fun loadFavorites() {
            val favorites = db.getFavoriteCars(userId)

            if (favorites.isEmpty()) {
                recycler.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                emptyState.visibility = View.GONE

                recycler.layoutManager = GridLayoutManager(requireContext(), 2)

                recycler.adapter = CarAdapter(
                    favorites.toMutableList(),
                    db,
                    {
                        loadFavorites()
                    },
                    { car ->
                        openCarDetail(car.id)
                    }
                )
            }
        }

        loadFavorites()

        favoriteListener = {
            view.post { loadFavorites() }
        }

        btnAddFav.setOnClickListener {
            findNavController().navigate(R.id.nav_collection)
        }
    }

    override fun onResume() {
        super.onResume()
        FavoriteEvents.addListener(favoriteListener)
    }

    override fun onPause() {
        super.onPause()
        FavoriteEvents.removeListener(favoriteListener)
    }
}