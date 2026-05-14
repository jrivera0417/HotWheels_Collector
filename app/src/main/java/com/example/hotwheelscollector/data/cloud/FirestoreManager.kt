package com.example.hotwheelscollector.data.cloud

import android.content.Context
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.example.hotwheelscollector.data.local.SyncPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreManager(
    private val context: Context
) {

    private val firestore =
        FirebaseFirestore.getInstance()

    private val auth =
        FirebaseAuth.getInstance()

    private val db =
        DatabaseHelper(context)

    // =========================
    // CURRENT USER UID
    // =========================
    private fun getUid(): String? {

        return auth.currentUser?.uid
    }

    // =========================
    // SYNC ALL DATA
    // =========================
    fun syncAllToCloud(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = getUid()

        SyncStatusManager.setState(
            SyncState.SYNCING
        )

        if (uid == null) {

            SyncStatusManager.setState(
                SyncState.ERROR
            )

            onError("Usuario no autenticado")
            return
        }

        val localUserId =
            SessionManager.getCurrentUserId(context)

        val collections =
            db.getCollectionsByUser(localUserId)

        val cars =
            db.getCarsByUser(localUserId)

        // =========================
        // SUBIR COLECCIONES
        // =========================
        collections.forEach { collection ->

            val collectionMap =
                hashMapOf(
                    "id" to collection.id,
                    "name" to collection.name,
                    "totalCars" to collection.totalCars,
                    "updatedAt" to collection.updatedAt
                )

            firestore.collection("users")
                .document(uid)
                .collection("collections")
                .document(collection.id.toString())
                .set(collectionMap)
        }

        // =========================
        // SUBIR CARROS
        // =========================
        cars.forEach { car ->

            val carMap =
                hashMapOf(
                    "id" to car.id,
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
                .set(carMap)
        }

        SyncPreferences.saveLastSync(context)

        SyncStatusManager.setState(
            SyncState.SYNCED
        )

        onSuccess()
    }

    // =========================
    // RESTORE FROM CLOUD
    // =========================
    fun restoreFromCloud(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = getUid()

        SyncStatusManager.setState(
            SyncState.SYNCING
        )

        if (uid == null) {

            onError("Usuario no autenticado")
            return
        }

        val localUserId =
            SessionManager.getCurrentUserId(context)

        // =========================
        // EVITAR DUPLICADOS
        // =========================
        val existingCars =
            db.getCarsByUser(localUserId)

        if (existingCars.isNotEmpty()) {

            SyncPreferences.saveLastSync(context)

            SyncStatusManager.setState(
                SyncState.SYNCED
            )

            onSuccess()
            return
        }

        // =========================
        // DESCARGAR COLECCIONES
        // =========================
        firestore.collection("users")
            .document(uid)
            .collection("collections")
            .get()
            .addOnSuccessListener { collectionsResult ->

                collectionsResult.documents.forEach { doc ->

                    val collection =
                        Collection(
                            id = doc.getLong("id")?.toInt() ?: 0,
                            userId = localUserId,
                            name = doc.getString("name") ?: "",
                            totalCars = doc.getLong("totalCars")?.toInt() ?: 0,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )

                    db.updateCollectionIfNewer(collection)
                }

                // =========================
                // DESCARGAR CARROS
                // =========================
                firestore.collection("users")
                    .document(uid)
                    .collection("cars")
                    .get()
                    .addOnSuccessListener { carsResult ->

                        carsResult.documents.forEach { doc ->

                            val car =
                                Car(
                                    id = doc.getLong("id")?.toInt() ?: 0,
                                    userId = localUserId,
                                    collectionId =
                                        doc.getLong("collectionId")
                                            ?.toInt() ?: 0,

                                    modelCode =
                                        doc.getString("modelCode") ?: "",

                                    name =
                                        doc.getString("name") ?: "",

                                    brand =
                                        doc.getString("brand") ?: "",

                                    seriesNumber =
                                        doc.getString("seriesNumber") ?: "",

                                    collectionNumber =
                                        doc.getString("collectionNumber") ?: "",

                                    color =
                                        doc.getString("color") ?: "",

                                    vehicleType =
                                        doc.getString("vehicleType") ?: "",

                                    purchaseDate =
                                        doc.getString("purchaseDate") ?: "",

                                    price =
                                        doc.getDouble("price") ?: 0.0,

                                    store =
                                        doc.getString("store") ?: "",

                                    quantity =
                                        doc.getLong("quantity")
                                            ?.toInt() ?: 0,

                                    favorite =
                                        doc.getBoolean("favorite") ?: false,

                                    imageUrl =
                                        doc.getString("imageUrl") ?: "",

                                    updatedAt =
                                        doc.getLong("updatedAt")
                                            ?: 0L
                                )

                            db.updateCarIfNewer(car)
                        }

                        SyncPreferences.saveLastSync(context)

                        SyncStatusManager.setState(
                            SyncState.SYNCED
                        )

                        onSuccess()
                    }
                    .addOnFailureListener {

                        SyncStatusManager.setState(
                            SyncState.ERROR
                        )

                        onError(
                            it.message
                                ?: "Error restaurando carros"
                        )
                    }
            }
            .addOnFailureListener {

                SyncStatusManager.setState(
                    SyncState.ERROR
                )

                onError(
                    it.message
                        ?: "Error restaurando colecciones"
                )
            }
    }
}