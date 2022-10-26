package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterCategory::class,
            childColumns = ["masterCategoryID"],
            parentColumns = ["id"]
        ),
        ForeignKey(
            entity = Subcategory::class,
            childColumns = ["subcategoryID"],
            parentColumns = ["id"]
        )
    ]
)
data class Product(
    @PrimaryKey val id: String,
    val masterCategoryID: String,
    val subcategoryID: String,
    val name: String = "",
    val price: Float = 1.0f,
    val discount: Float = 0.45f,
    val stringField1: String = "",
    val numericField1: String = ""
)