package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["recurrenceSeriesId", "scheduledDate"], unique = true)],
)
data class TaskEntity(
    @PrimaryKey
    val localId: String,
    val title: String,
    val description: String? = null,
    val category: String = "PERSONAL",
    val priority: String = "NORMAL",
    val notes: String? = null,
    val scheduledDate: String? = null,
    val scheduledTimeMinutes: Int? = null,
    val recurrenceFrequency: String = "NONE",
    val recurrenceInterval: Int = 1,
    val recurrenceEndDate: String? = null,
    val recurrenceSeriesId: String? = null,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    @Embedded
    val syncMetadata: SyncMetadata,
)
