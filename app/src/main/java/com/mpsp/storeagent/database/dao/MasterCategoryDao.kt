package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.MasterCategory

@Dao
interface MasterCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterCategory(masterCategory: List<MasterCategory>)

    @Query("SELECT * FROM MasterCategory")
    suspend fun getMasterCategories(): List<MasterCategory>

    @Query("SELECT * FROM MasterCategory WHERE MasterCategory.id = :masterCategoryID")
    suspend fun getMasterCategory(masterCategoryID: String): MasterCategory
}