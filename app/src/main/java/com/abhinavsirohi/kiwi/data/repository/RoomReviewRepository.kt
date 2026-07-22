package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskPostponementEntity
import com.abhinavsirohi.kiwi.data.local.entity.WeeklyReflectionEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.NewWeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.repository.ReviewRepository
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomReviewRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
) : ReviewRepository {
    constructor(
        database: KiwiDatabase,
        client: RemoteResult<SupabaseClient>,
        deviceId: String,
    ) : this(database, sessionProvider(client), deviceId)

    override fun observePostponements(): Flow<AppResult<List<TaskPostponement>>> {
        val userId = userId() ?: return flowOf(authFailure())
        return database.reviewDao().observePostponements(userId)
            .map<List<TaskPostponementEntity>, AppResult<List<TaskPostponement>>> { events ->
                AppResult.Success(events.map(TaskPostponementEntity::toDomain))
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override fun observeReflections(): Flow<AppResult<List<WeeklyReflection>>> {
        val userId = userId() ?: return flowOf(authFailure())
        return database.reviewDao().observeReflections(userId)
            .map<List<WeeklyReflectionEntity>, AppResult<List<WeeklyReflection>>> { reflections ->
                AppResult.Success(reflections.map(WeeklyReflectionEntity::toDomain))
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun saveReflection(reflection: NewWeeklyReflection): AppResult<WeeklyReflection> {
        val userId = userId() ?: return authFailure()
        val timestamp = now()
        val existing = database.reviewDao().findReflection(userId, reflection.weekStart)
        val entity = WeeklyReflectionEntity(
            localId = existing?.localId ?: newLocalId(),
            weekStart = reflection.weekStart,
            content = reflection.content.trim(),
            syncMetadata = existing?.syncMetadata?.copy(
                updatedAt = timestamp,
                deletedAt = null,
                syncStatus = SyncStatus.PENDING,
                lastSyncError = null,
                deviceId = deviceId,
            ) ?: SyncMetadata(
                userId = userId,
                createdAt = timestamp,
                updatedAt = timestamp,
                deviceId = deviceId,
            ),
        )
        return try {
            database.withTransaction {
                database.reviewDao().upsertReflection(entity)
                database.pendingChangeDao().enqueue(
                    PendingChangeEntity(
                        queueId = "WEEKLY_REFLECTION:${entity.localId}",
                        recordType = SyncRecordType.WEEKLY_REFLECTION,
                        recordLocalId = entity.localId,
                        operation = SyncOperation.UPSERT,
                        createdAt = entity.syncMetadata.createdAt,
                        updatedAt = entity.syncMetadata.updatedAt,
                    ),
                )
            }
            AppResult.Success(entity.toDomain())
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private fun userId() = (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId
    private fun authFailure() = AppResult.Failure(IllegalStateException("Authenticated session is required"))

    private companion object {
        fun sessionProvider(client: RemoteResult<SupabaseClient>): SessionProvider = when (client) {
            is RemoteResult.Success -> SupabaseSessionProvider(client.value)
            is RemoteResult.Failure -> object : SessionProvider { override fun currentSession() = client }
        }
    }
}

private fun TaskPostponementEntity.toDomain() = TaskPostponement(
    localId,
    taskLocalId,
    taskTitle,
    previousDate,
    newDate,
    postponedAt,
    syncMetadata.toDomain(),
)

private fun WeeklyReflectionEntity.toDomain() = WeeklyReflection(
    localId,
    weekStart,
    content,
    syncMetadata.toDomain(),
)

private fun SyncMetadata.toDomain() = RecordMetadata(
    remoteId,
    userId,
    createdAt,
    updatedAt,
    deletedAt,
    when (syncStatus) {
        SyncStatus.PENDING -> SyncState.Pending
        SyncStatus.SYNCED -> SyncState.Synced
        SyncStatus.FAILED -> SyncState.Failed
    },
    lastSyncError,
    deviceId,
)
