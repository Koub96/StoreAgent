package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.ProductAlias

@Dao
interface ProductAliasDao {
    @Query("SELECT * FROM ProductAlias WHERE ProductAlias.productID = :productID")
    suspend fun getProductAlias(productID: String): List<ProductAlias>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductAlias(productAlias: List<ProductAlias>)
}