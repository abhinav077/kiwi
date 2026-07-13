package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "cycle_records",
    indices = [
        Index(value = ["user_id", "start_date"]),
    ],
)
data class CycleRecordEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo(name = "end_date")
    val endDate: String? = null,
    @Embedded
    val syncMetadata: SyncMetadata,
)
