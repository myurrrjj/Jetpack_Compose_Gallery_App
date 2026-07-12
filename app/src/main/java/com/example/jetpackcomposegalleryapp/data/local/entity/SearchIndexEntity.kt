package com.example.jetpackcomposegalleryapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey


@Fts4
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,

    val mediaId: Long,

    val terms: String
)
