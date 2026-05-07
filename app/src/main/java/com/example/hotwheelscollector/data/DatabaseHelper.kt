package com.example.hotwheelscollector.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cars.db"
        const val DATABASE_VERSION = 4

        // TABLAS
        const val TABLE_USERS = "users"
        const val TABLE_CARS = "cars"
        const val TABLE_COLLECTIONS = "collections"

        // USERS
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_PASSWORD = "password"

        // COLLECTIONS
        const val COL_COLLECTION_ID = "id"
        const val COL_COLLECTION_USER_ID = "user_id"
        const val COL_COLLECTION_NAME = "name"
        const val COL_COLLECTION_TOTAL = "total_cars"

        // CARS
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
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT,
                $COL_USER_EMAIL TEXT UNIQUE,
                $COL_USER_PASSWORD TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_COLLECTIONS (
                $COL_COLLECTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_COLLECTION_USER_ID INTEGER,
                $COL_COLLECTION_NAME TEXT,
                $COL_COLLECTION_TOTAL INTEGER,
                FOREIGN KEY($COL_COLLECTION_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
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
                FOREIGN KEY($COL_CAR_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID),
                FOREIGN KEY($COL_COLLECTION_ID_FK) REFERENCES $TABLE_COLLECTIONS($COL_COLLECTION_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
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
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_USER_EMAIL = ?",
            arrayOf(email)
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    // =========================
    // COLLECTIONS
    // =========================
    fun insertCollection(userId: Int, name: String, totalCars: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_COLLECTION_USER_ID, userId)
            put(COL_COLLECTION_NAME, name)
            put(COL_COLLECTION_TOTAL, totalCars)
        }
        return db.insert(TABLE_COLLECTIONS, null, values)
    }

    fun getCollectionsByUser(userId: Int): List<Collection> {
        val list = mutableListOf<Collection>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_COLLECTIONS WHERE $COL_COLLECTION_USER_ID = ?",
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Collection(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_ID)),
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_USER_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLLECTION_NAME)),
                        totalCars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_TOTAL))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }

    fun getCollectionById(collectionId: Int): Collection? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_COLLECTIONS WHERE $COL_COLLECTION_ID = ?",
            arrayOf(collectionId.toString())
        )

        return if (cursor.moveToFirst()) {

            val collection = Collection(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLLECTION_NAME)),
                totalCars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLLECTION_TOTAL))
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
        }
        return db.insert(TABLE_CARS, null, values)
    }

    fun getCarsByUser(userId: Int): List<Car> {
        val list = mutableListOf<Car>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CARS WHERE $COL_CAR_USER_ID = ?",
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
            "SELECT * FROM $TABLE_CARS WHERE $COL_CAR_ID = ?",
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

    fun updateFavorite(carId: Int, isFavorite: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FAVORITE, if (isFavorite) 1 else 0)
        }

        db.update(
            TABLE_CARS,
            values,
            "$COL_CAR_ID = ?",
            arrayOf(carId.toString())
        )
    }

    fun getFavoriteCars(userId: Int): List<Car> {
        val list = mutableListOf<Car>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CARS WHERE $COL_CAR_USER_ID = ? AND $COL_FAVORITE = 1",
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

        val car = getCarById(carId) ?: return
        val collectionId = car.collectionId

        db.delete(
            TABLE_CARS,
            "$COL_CAR_ID = ?",
            arrayOf(carId.toString())
        )

        // 🔥 si la colección quedó vacía, la borras
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CARS WHERE $COL_COLLECTION_ID_FK = ?",
            arrayOf(collectionId.toString())
        )

        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
            db.delete(
                TABLE_COLLECTIONS,
                "$COL_COLLECTION_ID = ?",
                arrayOf(collectionId.toString())
            )
        }

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
        }

        return db.update(
            TABLE_CARS,
            values,
            "$COL_CAR_ID = ?",
            arrayOf(car.id.toString())
        )
    }

    // =========================
    // MAPPER SEGURO
    // =========================
    private fun mapCursorToCar(cursor: Cursor): Car {

        fun getStringSafe(column: String): String {
            return try {
                cursor.getString(cursor.getColumnIndexOrThrow(column)) ?: ""
            } catch (e: Exception) { "" }
        }

        fun getIntSafe(column: String): Int {
            return try {
                cursor.getInt(cursor.getColumnIndexOrThrow(column))
            } catch (e: Exception) { 0 }
        }

        fun getDoubleSafe(column: String): Double {
            return try {
                cursor.getDouble(cursor.getColumnIndexOrThrow(column))
            } catch (e: Exception) { 0.0 }
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
            imageUrl = getStringSafe(COL_IMAGE_URL)
        )
    }
}