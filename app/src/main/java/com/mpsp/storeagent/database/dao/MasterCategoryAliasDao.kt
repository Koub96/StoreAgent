package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.mpsp.storeagent.models.MasterCategoryAlias

@Dao
interface MasterCategoryAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterAlias(masterCategoryAlias: List<MasterCategoryAlias>)
}