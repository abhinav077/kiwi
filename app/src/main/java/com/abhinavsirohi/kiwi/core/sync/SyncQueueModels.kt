package com.abhinavsirohi.kiwi.core.sync

enum class SyncRecordType {
    TASK,
    SUBTASK,
}

enum class SyncOperation {
    UPSERT,
    DELETE,
}

enum class SyncQueueState {
    PENDING,
    PROCESSING,
    FAILED,
}

sealed interface SyncProcessingResult {
    data object QueueDrained : SyncProcessingResult

    data class Retry(val cause: Throwable? = null) : SyncProcessingResult

    data class PermanentFailure(val cause: Throwable? = null) : SyncProcessingResult
}

fun interface SyncProcessor {
    suspend fun processPendingChanges(): SyncProcessingResult
}
