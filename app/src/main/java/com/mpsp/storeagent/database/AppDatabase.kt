package com.mpsp.storeagent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mpsp.storeagent.database.dao.*
import com.mpsp.storeagent.models.*

@Database(
    entities = [
        Product::class,
        ProductAlias::class,
        Subcategory::class,
        MasterCategory::class,
        MasterCategoryAlias::class,
        SubcategoryAlias::class,
        MasterSubcategory::class
    ]
    , version = 1
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun ProductDao(): ProductDao
    abstract fun ProductAliasDao(): ProductAliasDao
    abstract fun SubcategoryDao(): SubcategoryDao
    abstract fun MasterCategoryDao(): MasterCategoryDao
    abstract fun MasterCategoryAliasDao(): MasterCategoryAliasDao
    abstract fun SubcategoryAliasDao(): SubcategoryAliasDao
    abstract fun MasterSubcategoryDao(): MasterSubcategoryDao
}