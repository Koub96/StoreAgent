package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Subcategory::class,
        childColumns = ["subcategoryID"],
        parentColumns = ["id"]
    )]
)
data class SubcategoryAlias(
    @PrimaryKey val id: String,
    val subcategoryID: String,
    val alias: String = ""
)
