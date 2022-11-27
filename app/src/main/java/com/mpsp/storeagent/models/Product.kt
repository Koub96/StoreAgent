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
    val price: Double = 1.0,
    val discount: Double = 0.45,
    val stringField1: String = "",
    val numericField1: String = ""
)