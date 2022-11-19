package com.mpsp.storeagent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpsp.storeagent.models.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    suspend fun getProducts(): List<Product>

    @Query("SELECT * FROM product WHERE product.subcategoryID = :subcategoryId")
    suspend fun getProductsBySubcategory(subcategoryId: String): List<Product>

    @Query("SELECT * FROM product WHERE product.name = :name")
    suspend fun getProductByName(name: String): Product?

    @Query("SELECT id FROM product WHERE product.name = :name")
    suspend fun getProductIdByName(name: String): String?

    @Query("SELECT * FROM product WHERE product.id = :id")
    suspend fun getProductById(id: String): Product

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)
}