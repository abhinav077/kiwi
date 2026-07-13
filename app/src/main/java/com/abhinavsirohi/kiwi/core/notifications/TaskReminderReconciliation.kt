package com.abhinavsirohi.kiwi.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.data.local.dao.TaskDao
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task

fun interface ReminderTaskSource {
    suspend fun reminderCandidates(): List<Task>
}

class RoomReminderTaskSource(
    private val taskDao: TaskDao,
) : ReminderTaskSource {
    override suspend fun reminderCandidates(): List<Task> =
        taskDao.getReminderCandidates().map(TaskEntity::toReminderTask)
}

class TaskReminderReconciler(
    private val taskSource: ReminderTaskSource,
    private val reminderScheduler: TaskReminderScheduler,
) {
    suspend fun reconcile(): Int {
        val candidates = taskSource.reminderCandidates()
        candidates.forEach(reminderScheduler::schedule)
        return candidates.size
    }
}

class TaskReminderReconciliationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as KiwiApplication
        return runCatching {
            TaskReminderReconciler(
                RoomReminderTaskSource(application.database.taskDao()),
                application.taskReminderScheduler,
            ).reconcile()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}

object TaskReminderReconciliationWorkRequest {
    const val UNIQUE_WORK_NAME = "kiwi-reminder-reconciliation"

    fun create(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<TaskReminderReconciliationWorker>().build()

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            create(),
        )
    }
}

class TaskReminderReconciliationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RECONCILIATION_ACTIONS) {
            TaskReminderReconciliationWorkRequest.enqueue(context)
        }
    }

    private companion object {
        val RECONCILIATION_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

private fun TaskEntity.toReminderTask(): Task = Task(
    localId = localId,
    title = title,
    scheduledDate = scheduledDate,
    scheduledTimeMinutes = scheduledTimeMinutes,
    isCompleted = isCompleted,
    metadata = RecordMetadata(
        remoteId = syncMetadata.remoteId,
        userId = syncMetadata.userId,
        createdAt = syncMetadata.createdAt,
        updatedAt = syncMetadata.updatedAt,
        deletedAt = syncMetadata.deletedAt,
        syncState = when (syncMetadata.syncStatus) {
            SyncStatus.PENDING -> SyncState.Pending
            SyncStatus.SYNCED -> SyncState.Synced
            SyncStatus.FAILED -> SyncState.Failed
        },
        lastSyncError = syncMetadata.lastSyncError,
        deviceId = syncMetadata.deviceId,
    ),
)
