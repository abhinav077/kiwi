package com.abhinavsirohi.kiwi.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.data.local.AndroidDiaryPhotoLocalStore
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import io.github.jan.supabase.storage.storage

fun interface DiaryPhotoSyncScheduler {
    fun schedule()
}

class WorkManagerDiaryPhotoSyncScheduler(context: Context) : DiaryPhotoSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<DiaryPhotoSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object { const val WORK_NAME = "diary-photo-sync" }
}

class DiaryPhotoSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as KiwiApplication
        val client = (application.supabaseClient as? RemoteResult.Success)?.value ?: return Result.retry()
        val userId = (SupabaseSessionProvider(client).currentSession() as? RemoteResult.Success)?.value?.userId
            ?: return Result.retry()
        val dao = application.database.diaryDao()
        val localStore = AndroidDiaryPhotoLocalStore(applicationContext)
        val bucket = client.storage.from(BUCKET)

        for (photo in dao.getPhotosAwaitingSync(userId)) {
            try {
                val remotePath = requireNotNull(photo.remotePath) { "Diary photo remote path is missing" }
                if (photo.syncMetadata.deletedAt == null) {
                    bucket.upload(remotePath, localStore.readBytes(photo.localPath)) { upsert = true }
                } else {
                    bucket.delete(remotePath)
                    check(localStore.delete(photo.localPath)) { "Local diary photo could not be deleted" }
                }
                val synced = photo.copy(
                    syncMetadata = photo.syncMetadata.copy(
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncError = null,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                dao.upsertPhoto(synced)
                application.database.pendingChangeDao().markSuccessful("DIARY_PHOTO:${photo.localId}")
            } catch (throwable: Throwable) {
                dao.upsertPhoto(
                    photo.copy(
                        syncMetadata = photo.syncMetadata.copy(
                            syncStatus = SyncStatus.FAILED,
                            lastSyncError = throwable.message?.take(MAX_ERROR_LENGTH),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    ),
                )
                return Result.retry()
            }
        }
        return Result.success()
    }

    private companion object {
        const val BUCKET = "diary-photos"
        const val MAX_ERROR_LENGTH = 500
    }
}
