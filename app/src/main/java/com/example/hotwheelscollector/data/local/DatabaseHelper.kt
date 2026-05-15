package com.example.hotwheelscollector.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.hotwheelscollector.data.notifications.AppNotification
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.cloud.SyncManager
import com.example.hotwheelscollector.data.cloud.SyncState
import com.example.hotwheelscollector.data.cloud.SyncStatusManager
import com.example.hotwheelscollector.data.models.User

class DatabaseHelper(
    private val context: Context
) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {

        const val DATABASE_NAME = "cars.db"
        const val DATABASE_VERSION = 13

        // =========================
        // TABLAS
        // =========================
        const val TABLE_USERS = "users"
        const val TABLE_CARS = "cars"
        const val TABLE_COLLECTIONS = "collections"
        const val TABLE_NOTIFICATIONS = "notifications"

        // =========================
        // USERS
        // =========================
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_PASSWORD = "password"
        const val COL_USER_FIREBASE_UID = "firebase_uid"
        const val COL_USER_PROFILE_IMAGE = "profile_image"

        // =========================
        // COLLECTIONS
        // =========================
        const val COL_COLLECTION_ID = "id"
        const val COL_COLLECTION_USER_ID = "user_id"
        const val COL_COLLECTION_NAME = "name"
        const val COL_COLLECTION_TOTAL = "total_cars"
        const val COL_UPDATED_AT = "updated_at"

        // =========================
        // CARS
        // =========================
        const val COL_CAR_ID = "id"
        const val COL_CAR_USER_ID = "user_id"
        const val COL_COLLECTION_ID_FK = "collection_id"
        const val COL_MODEL_CODE = "model_code"
        const val COL_NAME = "name"
        const val COL_BRAND = "brand"
        const val COL_SERIES_NUMBER = "series_number"
        const val COL_COLLECTION_NUMBER = "collection_number"
        const val COL_COLOR = "color"
        const val COL_VEHICLE_TYPE = "vehicle_type"
        const val COL_PURCHASE_DATE = "purchase_date"
        const val COL_PRICE = "price"
        const val COL_STORE = "store"
        const val COL_QUANTITY = "quantity"
        const val COL_FAVORITE = "favorite"
        const val COL_IMAGE_URL = "image_url"
        const val COL_CAR_UPDATED_AT = "updated_at"

        // =========================
        // NOTIFICATIONS
        // =========================
        const val COL_NOTIFICATION_ID = "id"
        const val COL_NOTIFICATION_TITLE = "title"
        const val COL_NOTIFICATION_MESSAGE = "message"
        const val COL_NOTIFICATION_TIMESTAMP = "timestamp"
        const val COL_NOTIFICATION_IS_READ = "is_read"
    }

    // =========================
    // CREATE
    // =========================
    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT,
                $COL_USER_EMAIL TEXT UNIQUE,
                $COL_USER_PASSWORD TEXT,
                $COL_USER_FIREBASE_UID TEXT,
                $COL_USER_PROFILE_IMAGE TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_COLLECTIONS (
                $COL_COLLECTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_COLLECTION_USER_ID INTEGER,
                $COL_COLLECTION_NAME TEXT,
                $COL_COLLECTION_TOTAL INTEGER,
                $COL_UPDATED_AT LONG,
                FOREIGN KEY($COL_COLLECTION_USER_ID)
                REFERENCES $TABLE_USERS($COL_USER_ID)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_CARS (
                $COL_CAR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAR_USER_ID INTEGER,
                $COL_COLLECTION_ID_FK INTEGER,
                $COL_MODEL_CODE TEXT NOT NULL,
                $COL_NAME TEXT,
                $COL_BRAND TEXT,
                $COL_SERIES_NUMBER TEXT,
                $COL_COLLECTION_NUMBER TEXT,
                $COL_COLOR TEXT,
                $COL_VEHICLE_TYPE TEXT,
                $COL_PURCHASE_DATE TEXT,
                $COL_PRICE REAL,
                $COL_STORE TEXT,
                $COL_QUANTITY INTEGER,
                $COL_FAVORITE INTEGER,
                $COL_IMAGE_URL TEXT,
                $COL_CAR_UPDATED_AT LONG,
                FOREIGN KEY($COL_CAR_USER_ID)
                REFERENCES $TABLE_USERS($COL_USER_ID),
                FOREIGN KEY($COL_COLLECTION_ID_FK)
                REFERENCES $TABLE_COLLECTIONS($COL_COLLECTION_ID)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COL_NOTIFICATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIFICATION_TITLE TEXT,
                $COL_NOTIFICATION_MESSAGE TEXT,
                $COL_NOTIFICATION_TIMESTAMP LONG,
                $COL_NOTIFICATION_IS_READ INTEGER
            )
        """)
    }

    // =========================
    // UPGRADE
    // =========================
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 13) {

            db.execSQL(
                """
            ALTER TABLE $TABLE_USERS
            ADD COLUMN $COL_USER_PROFILE_IMAGE TEXT
            """.trimIndent()
            )
        }
    }

    // =========================
    // USERS
    // =========================
    fun insertUser(user: User): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_USER_NAME, user.name)
            put(COL_USER_EMAIL, user.email)
            put(COL_USER_PASSWORD, user.password)
            put(COL_USER_FIREBASE_UID, user.firebaseUid)
        }

        return db.insert(
            TABLE_USERS,
            null,
            values
        )
    }

    fun ensureGuestUser(): User {

        val existingUser =
            getUserByEmail("guest@local")

        if (existingUser != null) {
            return existingUser
        }

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_USER_NAME, "Invitado")
            put(COL_USER_EMAIL, "guest@local")
            put(COL_USER_PASSWORD, "")
            put(COL_USER_FIREBASE_UID, "")
        }

        val insertedId = db.insert(
            TABLE_USERS,
            null,
            values
        ).toInt()

        return User(
            id = insertedId,
            name = "Invitado",
            email = "guest@local",
            password = "",
            firebaseUid = ""
        )
    }

    fun getUserByEmail(email: String): User? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_USER_EMAIL = ?",
            arrayOf(email)
        )

        return if (cursor.moveToFirst()) {

            val user = User(
                id = cursor.getInt(
                    cursor.getColumnIndexOrThrow(COL_USER_ID)
                ),

                name = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_USER_NAME)
                ),

                email = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_USER_EMAIL)
                ),

                password = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_USER_PASSWORD)
                ),

                firebaseUid = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_FIREBASE_UID
                    )
                ) ?: ""
            )

            cursor.close()

            user

        } else {

            cursor.close()

            null
        }
    }

    fun getUserByFirebaseUid(
        firebaseUid: String
    ): User? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_USERS
            WHERE $COL_USER_FIREBASE_UID = ?
            """.trimIndent(),
            arrayOf(firebaseUid)
        )

        return if (cursor.moveToFirst()) {

            val user = User(

                id = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_ID
                    )
                ),

                name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_NAME
                    )
                ),

                email = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_EMAIL
                    )
                ),

                password = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_PASSWORD
                    )
                ),

                firebaseUid = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_USER_FIREBASE_UID
                    )
                ) ?: ""
            )

            cursor.close()

            user

        } else {

            cursor.close()

            null
        }
    }

    fun updateUserProfileImage(
        userId: Int,
        imageUri: String
    ) {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(
                COL_USER_PROFILE_IMAGE,
                imageUri
            )
        }

        db.update(
            TABLE_USERS,
            values,
            "$COL_USER_ID = ?",
            arrayOf(userId.toString())
        )
    }

    fun saveProfileImage(userId: Int, base64Image: String) {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(
                COL_USER_PROFILE_IMAGE,
                base64Image
            )
        }

        db.update(
            TABLE_USERS,
            values,
            "$COL_USER_ID = ?",
            arrayOf(userId.toString())
        )
    }

    fun getProfileImage(userId: Int): String? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
        SELECT $COL_USER_PROFILE_IMAGE
        FROM $TABLE_USERS
        WHERE $COL_USER_ID = ?
        """.trimIndent(),
            arrayOf(userId.toString())
        )

        var image: String? = null

        if (cursor.moveToFirst()) {

            image = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    COL_USER_PROFILE_IMAGE
                )
            )
        }

        cursor.close()

        return image
    }

    // =========================
    // COLLECTIONS
    // =========================
    fun insertCollection(
        userId: Int,
        name: String,
        totalCars: Int
    ): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_COLLECTION_USER_ID, userId)
            put(COL_COLLECTION_NAME, name)
            put(COL_COLLECTION_TOTAL, totalCars)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }

        val id = db.insert(
            TABLE_COLLECTIONS,
            null,
            values
        )

        // =========================
        // FIRESTORE SYNC
        // =========================
        if (id != -1L) {

            SyncStatusManager.setState(
                SyncState.PENDING
            )
            SyncManager.syncCollection(
                Collection(
                    id = id.toInt(),
                    userId = userId,
                    name = name,
                    totalCars = totalCars,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        return id
    }

    fun insertCollectionLocalOnly(
        collection: Collection
    ): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_COLLECTION_ID, collection.id)
            put(COL_COLLECTION_USER_ID, collection.userId)
            put(COL_COLLECTION_NAME, collection.name)
            put(COL_COLLECTION_TOTAL, collection.totalCars)
            put(COL_UPDATED_AT, collection.updatedAt)
        }

        return db.insertWithOnConflict(
            TABLE_COLLECTIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getCollectionsByUser(
        userId: Int
    ): List<Collection> {

        val list = mutableListOf<Collection>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_COLLECTIONS
            WHERE $COL_COLLECTION_USER_ID = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {

            do {

                list.add(
                    Collection(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                COL_COLLECTION_ID
                            )
                        ),

                        userId = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                COL_COLLECTION_USER_ID
                            )
                        ),

                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COL_COLLECTION_NAME
                            )
                        ),

                        totalCars = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                COL_COLLECTION_TOTAL
                            )
                        )
                    )
                )

            } while (cursor.moveToNext())
        }

        cursor.close()

        return list
    }

    fun getCollectionCount(userId: Int): Int {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
        SELECT COUNT(*)
        FROM $TABLE_COLLECTIONS
        WHERE $COL_COLLECTION_USER_ID = ?
        """.trimIndent(),
            arrayOf(userId.toString())
        )

        val count =
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }

        cursor.close()

        return count
    }

    fun getCarCount(userId: Int): Int {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
        SELECT COUNT(*)
        FROM $TABLE_CARS
        WHERE $COL_CAR_USER_ID = ?
        """.trimIndent(),
            arrayOf(userId.toString())
        )

        val count =
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }

        cursor.close()

        return count
    }

    fun getCollectionById(
        collectionId: Int
    ): Collection? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_COLLECTIONS
            WHERE $COL_COLLECTION_ID = ?
            """.trimIndent(),
            arrayOf(collectionId.toString())
        )

        return if (cursor.moveToFirst()) {

            val collection = Collection(

                id = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        COL_COLLECTION_ID
                    )
                ),

                userId = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        COL_COLLECTION_USER_ID
                    )
                ),

                name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COL_COLLECTION_NAME
                    )
                ),

                totalCars = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        COL_COLLECTION_TOTAL
                    )
                )
            )

            cursor.close()

            collection

        } else {

            cursor.close()

            null
        }
    }

    // =========================
    // CARS
    // =========================
    fun insertCar(car: Car): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            // IMPORTANTE:
            // SOLO insertar ID si ya existe
            if (car.id > 0) {
                put(COL_CAR_ID, car.id)
            }

            put(COL_CAR_USER_ID, car.userId)
            put(COL_COLLECTION_ID_FK, car.collectionId)
            put(COL_MODEL_CODE, car.modelCode)
            put(COL_NAME, car.name)
            put(COL_BRAND, car.brand)
            put(COL_SERIES_NUMBER, car.seriesNumber)
            put(COL_COLLECTION_NUMBER, car.collectionNumber)
            put(COL_COLOR, car.color)
            put(COL_VEHICLE_TYPE, car.vehicleType)
            put(COL_PURCHASE_DATE, car.purchaseDate)
            put(COL_PRICE, car.price)
            put(COL_STORE, car.store)
            put(COL_QUANTITY, car.quantity)
            put(COL_FAVORITE, if (car.favorite) 1 else 0)
            put(COL_IMAGE_URL, car.imageUrl)
            put(COL_CAR_UPDATED_AT, System.currentTimeMillis())
        }

        val insertedId = db.insert(
            TABLE_CARS,
            null,
            values
        )

        if (insertedId != -1L) {

            val finalCar = car.copy(
                id = if (car.id > 0)
                    car.id
                else
                    insertedId.toInt()
            )

            Log.d(
                "CAR_ID_DEBUG",
                "Inserted car ${finalCar.name} with ID=${finalCar.id}"
            )

            SyncStatusManager.setState(
                SyncState.PENDING
            )

            SyncManager.syncCar(
                finalCar
            )
        }

        return insertedId
    }

    fun insertCarLocalOnly(car: Car): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_CAR_ID, car.id)
            put(COL_CAR_USER_ID, car.userId)
            put(COL_COLLECTION_ID_FK, car.collectionId)
            put(COL_MODEL_CODE, car.modelCode)
            put(COL_NAME, car.name)
            put(COL_BRAND, car.brand)
            put(COL_SERIES_NUMBER, car.seriesNumber)
            put(COL_COLLECTION_NUMBER, car.collectionNumber)
            put(COL_COLOR, car.color)
            put(COL_VEHICLE_TYPE, car.vehicleType)
            put(COL_PURCHASE_DATE, car.purchaseDate)
            put(COL_PRICE, car.price)
            put(COL_STORE, car.store)
            put(COL_QUANTITY, car.quantity)
            put(COL_FAVORITE, if (car.favorite) 1 else 0)
            put(COL_IMAGE_URL, car.imageUrl)
            put(COL_UPDATED_AT, car.updatedAt)
        }

        return db.insertWithOnConflict(
            TABLE_CARS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getCarsByUser(userId: Int): List<Car> {

        val list = mutableListOf<Car>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_CARS
            WHERE $COL_CAR_USER_ID = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {

            do {
                list.add(mapCursorToCar(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()

        return list
    }

    fun getCarById(carId: Int): Car? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_CARS
            WHERE $COL_CAR_ID = ?
            """.trimIndent(),
            arrayOf(carId.toString())
        )

        return if (cursor.moveToFirst()) {

            val car = mapCursorToCar(cursor)

            cursor.close()

            car

        } else {

            cursor.close()

            null
        }
    }

    fun updateFavorite(
        carId: Int,
        isFavorite: Boolean
    ) {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(
                COL_FAVORITE,
                if (isFavorite) 1 else 0
            )
        }

        db.update(
            TABLE_CARS,
            values,
            "$COL_CAR_ID = ?",
            arrayOf(carId.toString())
        )

        getCarById(carId)?.let {

            SyncStatusManager.setState(
                SyncState.PENDING
            )
            SyncManager.syncCar(
                it
            )
        }
    }

    fun getFavoriteCars(
        userId: Int
    ): List<Car> {

        val list = mutableListOf<Car>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_CARS
            WHERE $COL_CAR_USER_ID = ?
            AND $COL_FAVORITE = 1
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {

            do {
                list.add(mapCursorToCar(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()

        return list
    }

    fun deleteCar(carId: Int) {

        val db = writableDatabase

        val car = getCarById(carId)
            ?: return

        val collectionId = car.collectionId

        db.delete(
            TABLE_CARS,
            "$COL_CAR_ID = ?",
            arrayOf(carId.toString())
        )

        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) FROM $TABLE_CARS
            WHERE $COL_COLLECTION_ID_FK = ?
            """.trimIndent(),
            arrayOf(collectionId.toString())
        )

        if (
            cursor.moveToFirst()
            && cursor.getInt(0) == 0
        ) {

            db.delete(
                TABLE_COLLECTIONS,
                "$COL_COLLECTION_ID = ?",
                arrayOf(collectionId.toString())
            )
            SyncManager.deleteCollection(collectionId)
        }

        SyncStatusManager.setState(
            SyncState.PENDING
        )
        SyncManager.deleteCar(carId)
        cursor.close()
    }

    fun updateCar(car: Car): Int {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_NAME, car.name)
            put(COL_COLOR, car.color)
            put(COL_PRICE, car.price)
            put(COL_STORE, car.store)
            put(COL_COLLECTION_ID_FK, car.collectionId)
            put(COL_COLLECTION_NUMBER, car.collectionNumber)
            put(COL_IMAGE_URL, car.imageUrl)
            put(COL_FAVORITE, if (car.favorite) 1 else 0)
            put(COL_CAR_UPDATED_AT, System.currentTimeMillis())
        }

        val result = db.update(
            TABLE_CARS,
            values,
            "$COL_CAR_ID = ?",
            arrayOf(car.id.toString())
        )

        // =========================
        // FIRESTORE SYNC
        // =========================
        if (result > 0) {

            val updatedCar = car.copy(
                updatedAt = System.currentTimeMillis()
            )

            SyncStatusManager.setState(
                SyncState.PENDING
            )
            SyncManager.syncCar(
                updatedCar
            )
        }

        return result
    }

    fun updateCarIfNewer(car: Car) {

        val existing =
            getCarById(car.id)

        if (existing == null) {

            insertCarLocalOnly(car)
            return
        }

        if (car.updatedAt > existing.updatedAt) {

            insertCarLocalOnly(car)
        }
    }

    fun updateCollectionIfNewer(
        collection: Collection
    ) {

        val existing =
            getCollectionById(collection.id)

        if (existing == null) {

            insertCollectionLocalOnly(collection)
            return
        }

        if (
            collection.updatedAt >
            existing.updatedAt
        ) {

            insertCollectionLocalOnly(collection)
        }
    }

    // =========================
    // MIGRAR DATOS INVITADO
    // =========================
    fun migrateGuestDataToUser(newUserId: Int) {

        val guestUser =
            getUserByEmail("guest@local")
                ?: return

        val guestId = guestUser.id

        val db = writableDatabase

        // =========================
        // MOVER COLECCIONES
        // =========================
        val collectionValues = ContentValues().apply {

            put(COL_COLLECTION_USER_ID, newUserId)
        }

        db.update(
            TABLE_COLLECTIONS,
            collectionValues,
            "$COL_COLLECTION_USER_ID = ?",
            arrayOf(guestId.toString())
        )

        // =========================
        // MOVER CARROS
        // =========================
        val carValues = ContentValues().apply {

            put(COL_CAR_USER_ID, newUserId)
        }

        db.update(
            TABLE_CARS,
            carValues,
            "$COL_CAR_USER_ID = ?",
            arrayOf(guestId.toString())
        )
    }

    // =========================
    // NOTIFICATIONS
    // =========================
    fun insertNotification(
        notification: AppNotification
    ): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_NOTIFICATION_TITLE, notification.title)
            put(COL_NOTIFICATION_MESSAGE, notification.message)
            put(COL_NOTIFICATION_TIMESTAMP, notification.timestamp)

            put(
                COL_NOTIFICATION_IS_READ,
                if (notification.isRead) 1 else 0
            )
        }

        return db.insert(
            TABLE_NOTIFICATIONS,
            null,
            values
        )
    }

    fun getAllNotifications():
            List<AppNotification> {

        val list =
            mutableListOf<AppNotification>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NOTIFICATIONS
            ORDER BY $COL_NOTIFICATION_TIMESTAMP DESC
            """.trimIndent(),
            null
        )

        if (cursor.moveToFirst()) {

            do {

                list.add(
                    AppNotification(

                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                COL_NOTIFICATION_ID
                            )
                        ),

                        title = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COL_NOTIFICATION_TITLE
                            )
                        ),

                        message = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COL_NOTIFICATION_MESSAGE
                            )
                        ),

                        timestamp = cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                COL_NOTIFICATION_TIMESTAMP
                            )
                        ),

                        isRead =
                            cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                    COL_NOTIFICATION_IS_READ
                                )
                            ) == 1
                    )
                )

            } while (cursor.moveToNext())
        }

        cursor.close()

        return list
    }

    fun hasUnreadNotifications(): Boolean {

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) FROM $TABLE_NOTIFICATIONS
            WHERE $COL_NOTIFICATION_IS_READ = 0
            """.trimIndent(),
            null
        )

        val hasUnread =
            cursor.moveToFirst()
                    && cursor.getInt(0) > 0

        cursor.close()

        return hasUnread
    }

    fun markAllNotificationsAsRead() {

        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_NOTIFICATION_IS_READ, 1)
        }

        db.update(
            TABLE_NOTIFICATIONS,
            values,
            null,
            null
        )
    }

    fun deleteAllNotifications() {

        val db = writableDatabase

        db.delete(
            TABLE_NOTIFICATIONS,
            null,
            null
        )
    }

    // =========================
    // RESTORE COLLECTION
    // =========================
    fun insertCollectionObject(
        collection: Collection
    ): Long {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_COLLECTION_ID, collection.id)
            put(COL_COLLECTION_USER_ID, collection.userId)
            put(COL_COLLECTION_NAME, collection.name)
            put(COL_COLLECTION_TOTAL, collection.totalCars)
            put(COL_UPDATED_AT, collection.updatedAt)
        }

        return db.insertWithOnConflict(
            TABLE_COLLECTIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // =========================
    // MAPPER
    // =========================
    private fun mapCursorToCar(
        cursor: Cursor
    ): Car {

        fun getStringSafe(column: String): String {

            return try {

                cursor.getString(
                    cursor.getColumnIndexOrThrow(column)
                ) ?: ""

            } catch (e: Exception) {
                ""
            }
        }

        fun getIntSafe(column: String): Int {

            return try {

                cursor.getInt(
                    cursor.getColumnIndexOrThrow(column)
                )

            } catch (e: Exception) {
                0
            }
        }

        fun getDoubleSafe(column: String): Double {

            return try {

                cursor.getDouble(
                    cursor.getColumnIndexOrThrow(column)
                )

            } catch (e: Exception) {
                0.0
            }
        }

        fun getLongSafe(column: String): Long {

            return try {

                cursor.getLong(
                    cursor.getColumnIndexOrThrow(column)
                )

            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        return Car(

            id = getIntSafe(COL_CAR_ID),
            userId = getIntSafe(COL_CAR_USER_ID),
            collectionId = getIntSafe(COL_COLLECTION_ID_FK),
            modelCode = getStringSafe(COL_MODEL_CODE),
            name = getStringSafe(COL_NAME),
            brand = getStringSafe(COL_BRAND),
            seriesNumber = getStringSafe(COL_SERIES_NUMBER),
            collectionNumber = getStringSafe(COL_COLLECTION_NUMBER),
            color = getStringSafe(COL_COLOR),
            vehicleType = getStringSafe(COL_VEHICLE_TYPE),
            purchaseDate = getStringSafe(COL_PURCHASE_DATE),
            price = getDoubleSafe(COL_PRICE),
            store = getStringSafe(COL_STORE),
            quantity = getIntSafe(COL_QUANTITY),
            favorite = getIntSafe(COL_FAVORITE) == 1,
            imageUrl = getStringSafe(COL_IMAGE_URL),
            updatedAt = getLongSafe(COL_CAR_UPDATED_AT)
        )
    }
}