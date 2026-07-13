package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncQueueState
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType

@Entity(
    tableName = "pending_changes",
    indices = [
        Index(value = ["record_type", "record_local_id"], unique = true),
        Index(value = ["state", "next_attempt_at", "updated_at"]),
    ],
)
data class PendingChangeEntity(
    @PrimaryKey
    @ColumnInfo(name = "queue_id")
    val queueId: String,
    @ColumnInfo(name = "record_type")
    val recordType: SyncRecordType,
    @ColumnInfo(name = "record_local_id")
    val recordLocalId: String,
    val operation: SyncOperation,
    val state: SyncQueueState = SyncQueueState.PENDING,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,
    @ColumnInfo(name = "next_attempt_at")
    val nextAttemptAt: Long = createdAt,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
)
