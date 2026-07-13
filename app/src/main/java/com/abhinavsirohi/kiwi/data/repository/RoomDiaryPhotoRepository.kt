package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.DiaryPhotoSyncScheduler
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.DiaryPhotoLocalStore
import com.abhinavsirohi.kiwi.data.local.entity.DiaryPhotoEntity
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.DiaryPhoto
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.repository.DiaryPhotoRepository
import java.util.UUID
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomDiaryPhotoRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val localStore: DiaryPhotoLocalStore,
    private val syncScheduler: DiaryPhotoSyncScheduler,
    private val now: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
) : DiaryPhotoRepository {
    constructor(
        database: KiwiDatabase,
        client: RemoteResult<SupabaseClient>,
        deviceId: String,
        localStore: DiaryPhotoLocalStore,
        syncScheduler: DiaryPhotoSyncScheduler,
    ) : this(
        database = database,
        sessionProvider = when (client) {
            is RemoteResult.Success -> SupabaseSessionProvider(client.value)
            is RemoteResult.Failure -> object : SessionProvider { override fun currentSession() = client }
        },
        deviceId = deviceId,
        localStore = localStore,
        syncScheduler = syncScheduler,
    )

    override fun observePhotos(): Flow<AppResult<List<DiaryPhoto>>> {
        val userId = userId() ?: return flowOf(authFailure())
        return database.diaryDao().observeActivePhotos(userId)
            .map<List<DiaryPhotoEntity>, AppResult<List<DiaryPhoto>>> { photos ->
                AppResult.Success(photos.map { it.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun addPhoto(diaryEntryLocalId: String, sourceUri: String): AppResult<DiaryPhoto> {
        val userId = userId() ?: return authFailure()
        val entry = database.diaryDao().find(diaryEntryLocalId)
            ?: return AppResult.Failure(IllegalArgumentException("Diary entry was not found"))
        if (entry.syncMetadata.userId != userId || entry.syncMetadata.deletedAt != null) {
            return AppResult.Failure(IllegalAccessException("Diary entry is unavailable"))
        }
        val localId = newLocalId()
        val stored = try {
            localStore.importCompressed(sourceUri, localId)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        val timestamp = now()
        val entity = DiaryPhotoEntity(
            localId = localId,
            diaryEntryLocalId = diaryEntryLocalId,
            localPath = stored.path,
            remotePath = "$userId/$diaryEntryLocalId/$localId.jpg",
            mimeType = stored.mimeType,
            width = stored.width,
            height = stored.height,
            byteSize = stored.byteSize,
            syncMetadata = SyncMetadata(userId = userId, createdAt = timestamp, updatedAt = timestamp, deviceId = deviceId),
        )
        return try {
            database.withTransaction {
                database.diaryDao().upsertPhoto(entity)
                enqueue(entity, SyncOperation.UPSERT)
            }
            syncScheduler.schedule()
            AppResult.Success(entity.toDomain())
        } catch (throwable: Throwable) {
            localStore.delete(stored.path)
            AppResult.Failure(throwable)
        }
    }

    override suspend fun deletePhoto(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = userId() ?: return authFailure()
        val existing = database.diaryDao().findPhoto(localId)
            ?: return AppResult.Failure(IllegalArgumentException("Diary photo was not found"))
        if (existing.syncMetadata.userId != userId) return AppResult.Failure(IllegalAccessException("Diary photo is unavailable"))
        val deleted = existing.copy(
            syncMetadata = existing.syncMetadata.copy(
                deletedAt = deletedAt,
                updatedAt = deletedAt,
                syncStatus = SyncStatus.PENDING,
                lastSyncError = null,
            ),
        )
        return try {
            database.withTransaction {
                database.diaryDao().upsertPhoto(deleted)
                enqueue(deleted, SyncOperation.DELETE)
            }
            syncScheduler.schedule()
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private suspend fun enqueue(photo: DiaryPhotoEntity, operation: SyncOperation) {
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "DIARY_PHOTO:${photo.localId}",
                recordType = SyncRecordType.DIARY_PHOTO,
                recordLocalId = photo.localId,
                operation = operation,
                createdAt = photo.syncMetadata.createdAt,
                updatedAt = photo.syncMetadata.updatedAt,
            ),
        )
    }

    private fun DiaryPhotoEntity.toDomain() = DiaryPhoto(
        localId, diaryEntryLocalId, localPath, remotePath, mimeType, width, height, byteSize,
        RecordMetadata(
            syncMetadata.remoteId, syncMetadata.userId, syncMetadata.createdAt, syncMetadata.updatedAt,
            syncMetadata.deletedAt,
            when (syncMetadata.syncStatus) {
                SyncStatus.PENDING -> SyncState.Pending
                SyncStatus.SYNCED -> SyncState.Synced
                SyncStatus.FAILED -> SyncState.Failed
            },
            syncMetadata.lastSyncError, syncMetadata.deviceId,
        ),
    )

    private fun userId() = (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId
    private fun authFailure() = AppResult.Failure(IllegalStateException("Authenticated session is required"))
}
