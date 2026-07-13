package com.abhinavsirohi.kiwi.domain.model

data class PlannerSyncState(
    val pendingCount: Int = 0,
    val processingCount: Int = 0,
    val failedCount: Int = 0,
) {
    val totalAwaitingSync: Int
        get() = pendingCount + processingCount + failedCount
}
