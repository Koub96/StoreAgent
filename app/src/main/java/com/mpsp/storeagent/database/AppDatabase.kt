package com.mpsp.storeagent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mpsp.storeagent.database.dao.ProductAliasDao
import com.mpsp.storeagent.database.dao.ProductDao
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.ProductAlias

@Database(entities = [Product::class, ProductAlias::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun ProductDao(): ProductDao
    abstract fun ProductAliasDao(): ProductAliasDao
}