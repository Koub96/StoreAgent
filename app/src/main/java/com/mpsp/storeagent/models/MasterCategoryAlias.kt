package com.mpsp.storeagent.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = MasterCategory::class,
        childColumns = ["masterCategoryID"],
        parentColumns = ["id"]
    )]
)
data class MasterCategoryAlias(
    @PrimaryKey val id: String,
    val masterCategoryID: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val alias: String = ""
)
