package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["localId"],
            childColumns = ["task_local_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_local_id")],
)
data class SubtaskEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "task_local_id")
    val taskLocalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    @Embedded
    val syncMetadata: SyncMetadata,
)
