package com.mpsp.storeagent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mpsp.storeagent.database.dao.CategoryDao
import com.mpsp.storeagent.database.dao.ProductAliasDao
import com.mpsp.storeagent.database.dao.ProductDao
import com.mpsp.storeagent.database.dao.SubcategoryDao
import com.mpsp.storeagent.models.Category
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.ProductAlias
import com.mpsp.storeagent.models.Subcategory

@Database(entities = [Product::class, ProductAlias::class, Subcategory::class, Category::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun ProductDao(): ProductDao
    abstract fun ProductAliasDao(): ProductAliasDao
    abstract fun SubcategoryDao(): SubcategoryDao
    abstract fun CategoryDao(): CategoryDao
}