package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Product::class,
        childColumns = ["productID"],
        parentColumns = ["id"]
    )]
)
data class ProductAlias(
    @PrimaryKey val id: String,
    val productID: String,
    val alias: String = ""
)