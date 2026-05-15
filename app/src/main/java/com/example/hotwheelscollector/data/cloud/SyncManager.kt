package com.example.hotwheelscollector.data.cloud

import android.content.Context
import android.util.Log
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.network.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SyncManager {

    private val firestore = FirebaseFirestore.getInstance()

    // =========================
    // FIREBASE UID SAFE ACCESS
    // =========================
    private fun getUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // =========================
    // INTERNET CHECK
    // =========================
    private fun hasInternet(context: Context): Boolean {
        return NetworkMonitor(context).isConnected()
    }

    // ======================================================
    // SYNC COLLECTION
    // ======================================================
    fun syncCollection(context: Context, collection: Collection) {

        val uid = getUid()

        // ❌ SIN CLOUD USER → GUARDAR PENDIENTE
        if (uid == null) {
            PendingSyncManager.addCollection(collection)
            Log.d("SYNC", "Collection saved as pending")
            return
        }

        if (!hasInternet(context)) {
            PendingSyncManager.addCollection(collection)
            Log.d("SYNC", "No internet - collection pending")
            return
        }

        val data = hashMapOf(
            "id" to collection.id,
            "userId" to collection.userId,
            "name" to collection.name,
            "totalCars" to collection.totalCars,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(uid)
            .collection("collections")
            .document(collection.id.toString())
            .set(data)
            .addOnSuccessListener {
                Log.d("SYNC", "Collection synced")
            }
            .addOnFailureListener {
                PendingSyncManager.addCollection(collection)
                Log.e("SYNC", "Collection sync failed → pending")
            }
    }

    // ======================================================
    // SYNC CAR
    // ======================================================
    fun syncCar(context: Context, car: Car) {

        val uid = getUid()

        if (uid == null) {
            PendingSyncManager.addCar(car)
            Log.d("SYNC", "Car saved as pending")
            return
        }

        if (!hasInternet(context)) {
            PendingSyncManager.addCar(car)
            Log.d("SYNC", "No internet - car pending")
            return
        }

        val data = hashMapOf(
            "id" to car.id,
            "userId" to car.userId,
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
            "updatedAt" to car.updatedAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(car.id.toString())
            .set(data)
            .addOnSuccessListener {
                Log.d("SYNC", "Car synced")
            }
            .addOnFailureListener {
                PendingSyncManager.addCar(car)
                Log.e("SYNC", "Car sync failed → pending")
            }
    }

    // ======================================================
    // DELETE CAR
    // ======================================================
    fun deleteCar(carId: Int) {

        val uid = getUid() ?: return

        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId.toString())
            .delete()
    }

    // ======================================================
    // DELETE COLLECTION
    // ======================================================
    fun deleteCollection(collectionId: Int) {

        val uid = getUid() ?: return

        firestore.collection("users")
            .document(uid)
            .collection("collections")
            .document(collectionId.toString())
            .delete()
    }

    // ======================================================
    // FLUSH PENDING SYNC (CUANDO EL USUARIO HACE LOGIN)
    // ======================================================
    fun flushPendingSync(context: Context) {

        val uid = getUid() ?: return

        Log.d("SYNC", "Flushing pending data for user $uid")

        PendingSyncManager.getPendingCars().forEach {
            syncCar(context, it)
        }

        PendingSyncManager.getPendingCollections().forEach {
            syncCollection(context, it)
        }

        PendingSyncManager.clearAll()
    }
}