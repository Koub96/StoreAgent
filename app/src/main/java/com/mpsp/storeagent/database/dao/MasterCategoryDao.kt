package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.mpsp.storeagent.models.MasterCategory

@Dao
interface MasterCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterCategory(masterCategory: List<MasterCategory>)
}