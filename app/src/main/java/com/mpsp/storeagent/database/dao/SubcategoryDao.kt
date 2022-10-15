package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.Subcategory

@Dao
interface SubcategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: List<Subcategory>)

    @Query("SELECT * from Subcategory")
    suspend fun getSubcategories(): List<Subcategory>
}