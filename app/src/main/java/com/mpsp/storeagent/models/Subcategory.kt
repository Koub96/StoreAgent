package com.mpsp.storeagent.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Subcategory (
    @PrimaryKey val id: String,
    val stringField1Title: String = "",
    val numericField1Title: String = ""
)