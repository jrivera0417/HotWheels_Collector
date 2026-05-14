package com.example.hotwheelscollector.data.backup

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileExporter(private val context: Context) {

    fun exportCollection(){

        try {
            val db = DatabaseHelper(context)
            val currentUserId = SessionManager.getCurrentUserId(context)
            val cars = db.getCarsByUser(currentUserId)

            // =========================
            // ROOT JSON
            // =========================
            val root = JSONObject()

            root.put(
                "exported_at",
                System.currentTimeMillis()
            )

            root.put(
                "app",
                "Hot Wheels Collector"
            )

            // =========================
            // COLLECTIONS
            // =========================
            val collectionsArray = JSONArray()

            val collections: List<Collection> =
                db.getCollectionsByUser(currentUserId)

            for (collection in collections) {

                val collectionObject = JSONObject()

                collectionObject.put(
                    "id",
                    collection.id
                )

                collectionObject.put(
                    "userId",
                    collection.userId
                )

                collectionObject.put(
                    "name",
                    collection.name
                )

                collectionObject.put(
                    "totalCars",
                    collection.totalCars
                )

                collectionObject.put(
                    "updatedAt",
                    collection.updatedAt
                )

                collectionsArray.put(collectionObject)
            }

            root.put(
                "collections",
                collectionsArray
            )

            // =========================
            // CARS
            // =========================
            val carsArray = JSONArray()

            cars.forEach { car ->

                val carObject = JSONObject()

                carObject.put("id", car.id)
                carObject.put("userId", car.userId)
                carObject.put("collectionId", car.collectionId)
                carObject.put("modelCode", car.modelCode)
                carObject.put("name", car.name)
                carObject.put("brand", car.brand)
                carObject.put("seriesNumber", car.seriesNumber)
                carObject.put("collectionNumber", car.collectionNumber)
                carObject.put("color", car.color)
                carObject.put("vehicleType", car.vehicleType)
                carObject.put("purchaseDate", car.purchaseDate)
                carObject.put("price", car.price)
                carObject.put("store", car.store)
                carObject.put("quantity", car.quantity)
                carObject.put("favorite", car.favorite)
                carObject.put("imageUrl", car.imageUrl)
                carObject.put("updatedAt", car.updatedAt)

                carsArray.put(carObject)
            }

            root.put(
                "cars",
                carsArray
            )

            // =========================
            // FILE NAME
            // =========================
            val formatter =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                )

            val fileName =
                "hotwheels_backup_${formatter.format(Date())}.json"

            // =========================
            // DIRECTORY
            // =========================
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)

            val writer = FileWriter(file)

            writer.write(root.toString(4))
            writer.flush()
            writer.close()

            Toast.makeText(
                context,
                "Colección exportada en Descargas",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                context,
                "Error exportando colección",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}