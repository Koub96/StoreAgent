package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.MasterCategoryAlias
import com.mpsp.storeagent.models.Subcategory
import com.mpsp.storeagent.models.SubcategoryAlias

@Dao
interface SubcategoryAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategoryAlias(subcategoryAlias: List<SubcategoryAlias>)

    @Query("SELECT * FROM SubcategoryAlias WHERE SubcategoryAlias.subcategoryID = :subcategoryID")
    fun getSubcategoryAlias(subcategoryID: String): List<SubcategoryAlias>

    @Query("SELECT subcategoryID FROM SubcategoryAlias WHERE SubcategoryAlias.alias = :subcategoryAlias")
    suspend fun getSubcategoryIdByAlias(subcategoryAlias: String): String?
}