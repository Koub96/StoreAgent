package com.mpsp.storeagent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mpsp.storeagent.database.dao.ProductDao
import com.mpsp.storeagent.models.Product

@Database(entities = [Product::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun ProductDao(): ProductDao
}