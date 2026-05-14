package com.example.hotwheelscollector.data.backup

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class FileImporter(
    private val context: Context
) {

    fun importCollection(uri: Uri) {

        try {

            val inputStream =
                context.contentResolver
                    .openInputStream(uri)

            val reader = BufferedReader(
                InputStreamReader(inputStream)
            )

            val jsonText =
                reader.readText()

            reader.close()

            val root =
                JSONObject(jsonText)

            val db =
                DatabaseHelper(context)

            val currentUserId =
                SessionManager.getCurrentUserId(
                    context
                )

            // =========================
            // COLLECTIONS
            // =========================
            val collectionsArray =
                root.getJSONArray("collections")

            for (i in 0 until collectionsArray.length()) {

                val obj =
                    collectionsArray.getJSONObject(i)

                val collection =
                    Collection(
                        id = obj.getInt("id"),
                        userId = currentUserId,
                        name = obj.getString("name"),
                        totalCars = obj.getInt("totalCars"),
                        updatedAt = obj.getLong("updatedAt")
                    )

                db.insertCollectionLocalOnly(
                    collection
                )
            }

            // =========================
            // CARS
            // =========================
            val carsArray =
                root.getJSONArray("cars")

            for (i in 0 until carsArray.length()) {

                val obj =
                    carsArray.getJSONObject(i)

                val car =
                    Car(
                        id = obj.getInt("id"),
                        userId = currentUserId,
                        collectionId = obj.getInt("collectionId"),
                        modelCode = obj.getString("modelCode"),
                        name = obj.getString("name"),
                        brand = obj.getString("brand"),
                        seriesNumber = obj.getString("seriesNumber"),
                        collectionNumber = obj.getString("collectionNumber"),
                        color = obj.getString("color"),
                        vehicleType = obj.getString("vehicleType"),
                        purchaseDate = obj.getString("purchaseDate"),
                        price = obj.getDouble("price"),
                        store = obj.getString("store"),
                        quantity = obj.getInt("quantity"),
                        favorite = obj.getBoolean("favorite"),
                        imageUrl = obj.getString("imageUrl"),
                        updatedAt = obj.getLong("updatedAt")
                    )

                db.insertCarLocalOnly(car)
            }

            Toast.makeText(
                context,
                "Respaldo importado correctamente",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                context,
                "Error importando respaldo",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}