package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.mpsp.storeagent.models.SubcategoryAlias

@Dao
interface SubcategoryAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategoryAlias(subcategoryAlias: List<SubcategoryAlias>)
}