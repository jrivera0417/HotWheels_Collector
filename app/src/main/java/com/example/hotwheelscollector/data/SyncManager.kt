package com.example.hotwheelscollector.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SyncManager {

    private val firestore =
        FirebaseFirestore.getInstance()

    private fun hasInternet(
        context: Context
    ): Boolean {

        return NetworkMonitor(context)
            .isConnected()
    }

    // =========================
    // SYNC COLLECTION
    // =========================
    fun syncCollection(collection: Collection) {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val data = hashMapOf(

            "id" to collection.id,
            "userId" to collection.userId,
            "name" to collection.name,
            "totalCars" to collection.totalCars,
            "updatedAt" to System.currentTimeMillis()
        )

        SyncStatusManager.setState(
            SyncState.SYNCING
        )

        firestore.collection("users")
            .document(firebaseUser.uid)
            .collection("collections")
            .document(collection.id.toString())
            .set(data)
            .addOnSuccessListener {

                SyncStatusManager.setState(
                    SyncState.SYNCED
                )

                Log.d(
                    "SYNC",
                    "Collection synced"
                )
            }
            .addOnFailureListener {

                SyncStatusManager.setState(
                    SyncState.ERROR
                )

                Log.e(
                    "SYNC",
                    "Collection sync failed"
                )
            }
    }

    // =========================
    // SYNC CAR
    // =========================
    fun syncCar(
        context: Context,
        car: Car
    ){

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        if (!hasInternet(context)) {

            SyncStatusManager.setState(
                SyncState.PENDING
            )

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

        SyncStatusManager.setState(
            SyncState.SYNCING
        )

        firestore.collection("users")
            .document(firebaseUser.uid)
            .collection("cars")
            .document(car.id.toString())
            .set(data)
            .addOnSuccessListener {

                SyncStatusManager.setState(
                    SyncState.SYNCED
                )

                Log.d(
                    "SYNC",
                    "Car synced"
                )
            }
            .addOnFailureListener {

                SyncStatusManager.setState(
                    SyncState.PENDING
                )

                Log.e(
                    "SYNC",
                    "Car sync failed"
                )
            }
    }

    // =========================
    // DELETE CAR
    // =========================
    fun deleteCar(carId: Int) {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        firestore.collection("users")
            .document(firebaseUser.uid)
            .collection("cars")
            .document(carId.toString())
            .delete()
    }

    // =========================
    // DELETE COLLECTION
    // =========================
    fun deleteCollection(collectionId: Int) {

        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        firestore.collection("users")
            .document(firebaseUser.uid)
            .collection("collections")
            .document(collectionId.toString())
            .delete()
    }
}