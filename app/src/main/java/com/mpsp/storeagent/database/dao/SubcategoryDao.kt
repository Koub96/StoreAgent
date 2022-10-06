package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.mpsp.storeagent.models.Subcategory

@Dao
interface SubcategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: List<Subcategory>)
}