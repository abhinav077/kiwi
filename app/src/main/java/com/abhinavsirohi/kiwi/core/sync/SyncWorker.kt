package com.abhinavsirohi.kiwi.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val syncProcessor: SyncProcessor,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = when (SyncRunner(syncProcessor).run()) {
        SyncWorkerOutcome.SUCCESS -> Result.success()
        SyncWorkerOutcome.RETRY -> Result.retry()
        SyncWorkerOutcome.FAILURE -> Result.failure()
    }
}

enum class SyncWorkerOutcome {
    SUCCESS,
    RETRY,
    FAILURE,
}

class SyncRunner(
    private val syncProcessor: SyncProcessor,
) {
    suspend fun run(): SyncWorkerOutcome = when (syncProcessor.processPendingChanges()) {
        SyncProcessingResult.QueueDrained -> SyncWorkerOutcome.SUCCESS
        is SyncProcessingResult.Retry -> SyncWorkerOutcome.RETRY
        is SyncProcessingResult.PermanentFailure -> SyncWorkerOutcome.FAILURE
    }
}

class SyncWorkerFactory(
    private val syncProcessor: SyncProcessor,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = if (workerClassName == SyncWorker::class.java.name) {
        SyncWorker(appContext, workerParameters, syncProcessor)
    } else {
        null
    }
}
