package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "task_postponements",
    indices = [
        Index(value = ["user_id", "postponed_at"]),
        Index(value = ["task_local_id"]),
    ],
)
data class TaskPostponementEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "task_local_id")
    val taskLocalId: String,
    @ColumnInfo(name = "task_title")
    val taskTitle: String,
    @ColumnInfo(name = "previous_date")
    val previousDate: String,
    @ColumnInfo(name = "new_date")
    val newDate: String,
    @ColumnInfo(name = "postponed_at")
    val postponedAt: Long,
    @Embedded
    val syncMetadata: SyncMetadata,
)

@Entity(
    tableName = "weekly_reflections",
    indices = [Index(value = ["user_id", "week_start"], unique = true)],
)
data class WeeklyReflectionEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "week_start")
    val weekStart: String,
    val content: String,
    @Embedded
    val syncMetadata: SyncMetadata,
)
