package com.abhinavsirohi.kiwi.core.sync

enum class SyncRecordType {
    TASK,
    SUBTASK,
    PROFILE,
    CYCLE_RECORD,
    WELLNESS_DAILY_RECORD,
    HEALTH_ALERT_EPISODE,
    DIARY_ENTRY,
    DIARY_PHOTO,
    SELF_CARE_ROUTINE,
    TASK_POSTPONEMENT,
    WEEKLY_REFLECTION,
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
