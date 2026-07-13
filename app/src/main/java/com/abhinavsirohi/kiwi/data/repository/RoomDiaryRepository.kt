package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.core.sync.DiaryPhotoSyncScheduler
import com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.NewDiaryEntry
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.repository.DiaryRepository
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomDiaryRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
    private val photoSyncScheduler: DiaryPhotoSyncScheduler = DiaryPhotoSyncScheduler {},
) : DiaryRepository {
    constructor(
        database: KiwiDatabase,
        client: RemoteResult<SupabaseClient>,
        deviceId: String,
        photoSyncScheduler: DiaryPhotoSyncScheduler = DiaryPhotoSyncScheduler {},
    ) : this(
        database,
        when (client) {
            is RemoteResult.Success -> SupabaseSessionProvider(client.value)
            is RemoteResult.Failure -> object : SessionProvider { override fun currentSession() = client }
        },
        deviceId,
        photoSyncScheduler = photoSyncScheduler,
    )

    override fun observeEntries(): Flow<AppResult<List<DiaryEntry>>> {
        val userId = authenticatedUserId() ?: return flowOf(authFailure())
        return database.diaryDao().observeActive(userId)
            .map<List<DiaryEntryEntity>, AppResult<List<DiaryEntry>>> { entries ->
                AppResult.Success(entries.map { it.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun createEntry(entry: NewDiaryEntry): AppResult<DiaryEntry> {
        val userId = authenticatedUserId() ?: return authFailure()
        val timestamp = now()
        val entity = DiaryEntryEntity(
            localId = newLocalId(),
            title = entry.title.trim(),
            content = entry.content.trim(),
            entryDate = entry.entryDate,
            bestThing = entry.bestThing.cleaned(),
            mood = entry.mood.cleaned(),
            isFavourite = entry.isFavourite,
            syncMetadata = SyncMetadata(userId = userId, createdAt = timestamp, updatedAt = timestamp, deviceId = deviceId),
        )
        return persist(entity)
    }

    override suspend fun saveEntry(entry: DiaryEntry): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authFailure()
        if (entry.title.isBlank() || entry.content.isBlank()) {
            return AppResult.Failure(IllegalArgumentException("Diary title and content are required"))
        }
        val existing = database.diaryDao().find(entry.localId)
            ?: return AppResult.Failure(IllegalArgumentException("Diary entry was not found"))
        if (existing.syncMetadata.userId != userId || entry.metadata.userId != userId) {
            return AppResult.Failure(IllegalAccessException("Diary entry is unavailable"))
        }
        val updated = entry.toEntity(existing.syncMetadata.pending(now()))
        return try {
            database.withTransaction {
                database.diaryDao().upsert(updated)
                enqueue(updated, SyncOperation.UPSERT)
            }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun tombstoneEntry(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authFailure()
        val existing = database.diaryDao().find(localId)
            ?: return AppResult.Failure(IllegalArgumentException("Diary entry was not found"))
        if (existing.syncMetadata.userId != userId) return AppResult.Failure(IllegalAccessException("Diary entry is unavailable"))
        val deleted = existing.copy(syncMetadata = existing.syncMetadata.copy(
            deletedAt = deletedAt,
            updatedAt = deletedAt,
            syncStatus = SyncStatus.PENDING,
            lastSyncError = null,
        ))
        return try {
            val attachedPhotos = database.diaryDao().getActivePhotosForEntry(localId, userId)
            database.withTransaction {
                database.diaryDao().upsert(deleted)
                enqueue(deleted, SyncOperation.DELETE)
                attachedPhotos.forEach { photo ->
                    val deletedPhoto = photo.copy(
                        syncMetadata = photo.syncMetadata.copy(
                            deletedAt = deletedAt,
                            updatedAt = deletedAt,
                            syncStatus = SyncStatus.PENDING,
                            lastSyncError = null,
                        ),
                    )
                    database.diaryDao().upsertPhoto(deletedPhoto)
                    database.pendingChangeDao().enqueue(
                        PendingChangeEntity(
                            queueId = "DIARY_PHOTO:${photo.localId}",
                            recordType = SyncRecordType.DIARY_PHOTO,
                            recordLocalId = photo.localId,
                            operation = SyncOperation.DELETE,
                            createdAt = photo.syncMetadata.createdAt,
                            updatedAt = deletedAt,
                        ),
                    )
                }
            }
            if (attachedPhotos.isNotEmpty()) photoSyncScheduler.schedule()
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private suspend fun persist(entity: DiaryEntryEntity): AppResult<DiaryEntry> = try {
        database.withTransaction {
            database.diaryDao().upsert(entity)
            enqueue(entity, SyncOperation.UPSERT)
        }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable)
    }

    private suspend fun enqueue(entity: DiaryEntryEntity, operation: SyncOperation) {
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "DIARY_ENTRY:${entity.localId}",
                recordType = SyncRecordType.DIARY_ENTRY,
                recordLocalId = entity.localId,
                operation = operation,
                createdAt = entity.syncMetadata.createdAt,
                updatedAt = entity.syncMetadata.updatedAt,
            ),
        )
    }

    private fun DiaryEntryEntity.toDomain() = DiaryEntry(
        localId, title, content, entryDate, bestThing, mood, isFavourite, syncMetadata.toDomain(),
    )

    private fun DiaryEntry.toEntity(metadata: SyncMetadata) = DiaryEntryEntity(
        localId, title.trim(), content.trim(), entryDate, bestThing.cleaned(), mood.cleaned(), isFavourite, metadata,
    )

    private fun SyncMetadata.pending(timestamp: Long) = copy(
        updatedAt = timestamp,
        syncStatus = SyncStatus.PENDING,
        lastSyncError = null,
        deviceId = deviceId,
    )

    private fun SyncMetadata.toDomain() = RecordMetadata(
        remoteId, userId, createdAt, updatedAt, deletedAt,
        when (syncStatus) {
            SyncStatus.PENDING -> SyncState.Pending
            SyncStatus.SYNCED -> SyncState.Synced
            SyncStatus.FAILED -> SyncState.Failed
        },
        lastSyncError, deviceId,
    )

    private fun String?.cleaned() = this?.trim()?.takeIf(String::isNotEmpty)
    private fun authenticatedUserId() = (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId
    private fun authFailure() = AppResult.Failure(IllegalStateException("Authenticated session is required"))
}
