package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val localId: String,
    val title: String,
    val notes: String? = null,
    val scheduledDate: String? = null,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    @Embedded
    val syncMetadata: SyncMetadata,
)
