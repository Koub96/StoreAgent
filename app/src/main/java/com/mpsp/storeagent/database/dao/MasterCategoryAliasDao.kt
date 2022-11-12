package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.MasterCategoryAlias
import com.mpsp.storeagent.models.ProductAlias

@Dao
interface MasterCategoryAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterAlias(masterCategoryAlias: List<MasterCategoryAlias>)

    @Query("SELECT * FROM MasterCategoryAlias WHERE MasterCategoryAlias.masterCategoryID = :masterCategoryID")
    fun getMasterCategoryAlias(masterCategoryID: String): List<MasterCategoryAlias>

    @Query("SELECT masterCategoryID FROM MasterCategoryAlias WHERE MasterCategoryAlias.alias = :masterCategoryAlias")
    suspend fun getMasterCategoryIdByAlias(masterCategoryAlias: String): String?
}