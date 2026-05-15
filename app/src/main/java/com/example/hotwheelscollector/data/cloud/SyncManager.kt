package com.example.hotwheelscollector.data.cloud

import android.content.Context
import android.util.Log
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SyncManager {

    private val firestore = FirebaseFirestore.getInstance()

    // =========================
    // SAFE GATE (CLAVE DEL SISTEMA)
    // =========================
    private fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun uid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // =========================
    // SYNC CAR
    // =========================
    fun syncCar(car: Car) {

        if (!isUserLoggedIn()) {
            Log.d("SYNC", "Blocked: user not logged in")
            return
        }

        val userId = uid() ?: return

        val data = hashMapOf(
            "id" to car.id,
            "userId" to userId,
            "collectionId" to car.collectionId,
            "modelCode" to car.modelCode,
            "name" to car.name,
            "brand" to car.brand,
            "seriesNumber" to car.seriesNumber,
            "collectionNumber" to car.collectionNumber,
            "color" to car.color,
            "vehicleType" to car.vehicleType,
            "purchaseDate" to car.purchaseDate,
            "price" to car.price,
            "store" to car.store,
            "quantity" to car.quantity,
            "favorite" to car.favorite,
            "imageUrl" to car.imageUrl,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("cars")
            .document(car.id.toString())
            .set(data)
            .addOnSuccessListener {
                Log.d("SYNC", "Car synced: ${car.name}")
            }
            .addOnFailureListener {
                Log.e("SYNC", "Car sync failed: ${car.name}")
            }
    }

    // =========================
    // SYNC COLLECTION
    // =========================
    fun syncCollection(collection: Collection) {

        if (!isUserLoggedIn()) {
            Log.d("SYNC", "Blocked: user not logged in")
            return
        }

        val userId = uid() ?: return

        val data = hashMapOf(
            "id" to collection.id,
            "userId" to userId,
            "name" to collection.name,
            "totalCars" to collection.totalCars,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("collections")
            .document(collection.id.toString())
            .set(data)
            .addOnSuccessListener {
                Log.d("SYNC", "Collection synced: ${collection.name}")
            }
            .addOnFailureListener {
                Log.e("SYNC", "Collection sync failed")
            }
    }

    // =========================
    // FULL SYNC (SAFE HYBRID)
    // =========================
    fun syncAllLocalData(context: Context) {

        if (!isUserLoggedIn()) {
            Log.d("SYNC", "Hybrid mode: guest user → sync skipped")
            return
        }

        val firebaseUid = uid() ?: return

        val db = DatabaseHelper(context)

        val localUserId = SessionManager.getCurrentUserId(context)

        val cars = db.getCarsByUser(localUserId)
        val collections = db.getCollectionsByUser(localUserId)

        if (cars.isEmpty() && collections.isEmpty()) {
            Log.d("SYNC", "Nothing to sync")
            return
        }

        cars.forEach { syncCar(it) }
        collections.forEach { syncCollection(it) }

        Log.d("SYNC", "Full sync completed for user: $firebaseUid")
    }

    // =========================
    // DELETE CAR
    // =========================
    fun deleteCar(carId: Int) {

        if (!isUserLoggedIn()) return

        val userId = uid() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("cars")
            .document(carId.toString())
            .delete()
    }

    // =========================
    // DELETE COLLECTION
    // =========================
    fun deleteCollection(collectionId: Int) {

        if (!isUserLoggedIn()) return

        val userId = uid() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("collections")
            .document(collectionId.toString())
            .delete()
    }
}