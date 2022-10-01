package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Product(
    @PrimaryKey val id: Int,
    val name: String = "",
    val price: Float = 1.0f,
    val discount: Float = 0.45f,
    val stringField1: String = "",
    val numericField1: String = ""
)