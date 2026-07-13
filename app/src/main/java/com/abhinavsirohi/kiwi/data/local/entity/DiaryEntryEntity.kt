package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "diary_entries",
    indices = [
        Index(value = ["user_id", "entry_date"]),
        Index(value = ["user_id", "updated_at"]),
    ],
)
data class DiaryEntryEntity(
    @PrimaryKey val localId: String,
    val title: String,
    val content: String,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "best_thing") val bestThing: String? = null,
    val mood: String? = null,
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean = false,
    @Embedded val syncMetadata: SyncMetadata,
)
