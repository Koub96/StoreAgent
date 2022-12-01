package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity
data class Basket(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val basketID: String,
    val productID: String,
    var quantity: Int = 1
)
