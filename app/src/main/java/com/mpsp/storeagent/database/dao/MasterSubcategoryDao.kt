package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.MasterSubcategory

@Dao
interface MasterSubcategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterSubcategory(masterSubcategory: List<MasterSubcategory>)

    @Query("SELECT * FROM MasterSubcategory")
    suspend fun getMasterSubcategories(): List<MasterSubcategory>
}