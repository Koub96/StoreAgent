package com.mpsp.storeagent.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["masterCategoryID","subcategoryID"])
data class MasterSubcategory(
    @ColumnInfo @NonNull
    val masterCategoryID: String,
    @ColumnInfo @NonNull
    val subcategoryID: String
)
