package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.Basket
import kotlinx.coroutines.flow.Flow

@Dao
interface BasketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBasketLine(basket: Basket)

    @Query("SELECT * FROM Basket WHERE Basket.basketID = :basketID")
    fun getBasketFlow(basketID: String): Flow<List<Basket>>

    @Query("SELECT * FROM Basket WHERE Basket.basketID = :basketID")
    suspend fun getBasket(basketID: String): List<Basket>
}