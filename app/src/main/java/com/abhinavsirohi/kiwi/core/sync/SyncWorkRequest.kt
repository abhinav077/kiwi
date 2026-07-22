package com.abhinavsirohi.kiwi.core.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit

object SyncWorkRequest {
    const val UNIQUE_WORK_NAME = "kiwi-pending-sync"

    fun create(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    fun enqueue(workManager: WorkManager) {
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, create())
    }
}
