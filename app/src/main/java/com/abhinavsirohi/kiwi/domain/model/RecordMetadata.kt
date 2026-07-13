package com.abhinavsirohi.kiwi.domain.model

data class RecordMetadata(
    val remoteId: String? = null,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.Pending,
    val lastSyncError: String? = null,
    val deviceId: String,
)

enum class SyncState {
    Pending,
    Synced,
    Failed,
}
