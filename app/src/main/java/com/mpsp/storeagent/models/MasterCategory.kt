package com.mpsp.storeagent.models

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.NOCASE
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MasterCategory(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = NOCASE)
    val title: String = ""
)
