package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.notifications.NoOpSelfCareReminderScheduler
import com.abhinavsirohi.kiwi.core.notifications.SelfCareReminderScheduler
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.NewSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SelfCareCategory
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.repository.SelfCareRepository
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomSelfCareRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
    private val reminderScheduler: SelfCareReminderScheduler = NoOpSelfCareReminderScheduler,
) : SelfCareRepository {
    constructor(database: KiwiDatabase, client: RemoteResult<SupabaseClient>, deviceId: String, reminderScheduler: SelfCareReminderScheduler = NoOpSelfCareReminderScheduler) : this(
        database,
        when (client) {
            is RemoteResult.Success -> SupabaseSessionProvider(client.value)
            is RemoteResult.Failure -> object : SessionProvider { override fun currentSession() = client }
        },
        deviceId,
        reminderScheduler = reminderScheduler,
    )

    override fun observeRoutines(): Flow<AppResult<List<SelfCareRoutine>>> {
        val userId = userId() ?: return flowOf(AppResult.Failure(IllegalStateException("Authenticated session is required")))
        return database.selfCareDao().observeActive(userId)
            .map<List<SelfCareRoutineEntity>, AppResult<List<SelfCareRoutine>>> { AppResult.Success(it.map { entity -> entity.toDomain() }) }
            .catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun createRoutine(routine: NewSelfCareRoutine): AppResult<SelfCareRoutine> {
        val user = userId() ?: return authFailure()
        val timestamp = now()
        val entity = SelfCareRoutineEntity(
            localId = newLocalId(), name = routine.name.trim(), description = routine.description?.trim(),
            category = routine.category.name, scheduledTimeMinutes = routine.scheduledTimeMinutes,
            repeatDays = encodeDays(routine.repeatDays), checklist = encodeList(routine.checklist),
            isActive = true, completionDates = "", syncMetadata = SyncMetadata(userId = user, createdAt = timestamp, updatedAt = timestamp, deviceId = deviceId),
        )
        return persist(entity, SyncOperation.UPSERT).also { if (it is AppResult.Success) reminderScheduler.schedule(it.value) }
    }

    override suspend fun saveRoutine(routine: SelfCareRoutine): AppResult<Unit> {
        val user = userId() ?: return authFailure()
        if (routine.name.isBlank()) return AppResult.Failure(IllegalArgumentException("Routine name is required"))
        val existing = database.selfCareDao().find(routine.localId) ?: return AppResult.Failure(IllegalArgumentException("Routine was not found"))
        if (existing.syncMetadata.userId != user || routine.metadata.userId != user) return AppResult.Failure(IllegalAccessException("Routine belongs to another user"))
        val updated = routine.toEntity(existing.syncMetadata.copy(updatedAt = now(), syncStatus = SyncStatus.PENDING, deletedAt = null, lastSyncError = null, deviceId = deviceId))
        return try {
            database.withTransaction { database.selfCareDao().upsert(updated); enqueue(updated, SyncOperation.UPSERT) }
            reminderScheduler.schedule(routine)
            AppResult.Success(Unit)
        } catch (throwable: Throwable) { AppResult.Failure(throwable) }
    }

    override suspend fun tombstoneRoutine(localId: String, deletedAt: Long): AppResult<Unit> {
        val user = userId() ?: return authFailure()
        val existing = database.selfCareDao().find(localId) ?: return AppResult.Failure(IllegalArgumentException("Routine was not found"))
        if (existing.syncMetadata.userId != user) return AppResult.Failure(IllegalAccessException("Routine belongs to another user"))
        val deleted = existing.copy(syncMetadata = existing.syncMetadata.copy(updatedAt = deletedAt, deletedAt = deletedAt, syncStatus = SyncStatus.PENDING, lastSyncError = null, deviceId = deviceId))
        return try {
            database.withTransaction { database.selfCareDao().upsert(deleted); enqueue(deleted, SyncOperation.DELETE) }
            reminderScheduler.cancel(localId)
            AppResult.Success(Unit)
        } catch (throwable: Throwable) { AppResult.Failure(throwable) }
    }

    private suspend fun persist(entity: SelfCareRoutineEntity, operation: SyncOperation): AppResult<SelfCareRoutine> = try {
        database.withTransaction { database.selfCareDao().upsert(entity); enqueue(entity, operation) }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) { AppResult.Failure(throwable) }

    private suspend fun enqueue(entity: SelfCareRoutineEntity, operation: SyncOperation) {
        database.pendingChangeDao().enqueue(PendingChangeEntity("SELF_CARE:${entity.localId}", SyncRecordType.SELF_CARE_ROUTINE, entity.localId, operation, createdAt = entity.syncMetadata.createdAt, updatedAt = entity.syncMetadata.updatedAt))
    }

    private fun SelfCareRoutineEntity.toDomain() = SelfCareRoutine(localId, name, description, SelfCareCategory.entries.firstOrNull { it.name == category } ?: SelfCareCategory.Mind, scheduledTimeMinutes, decodeDays(repeatDays), decodeList(checklist), isActive, decodeList(completionDates).toSet(), syncMetadata.toDomain())
    private fun SelfCareRoutine.toEntity(metadata: SyncMetadata) = SelfCareRoutineEntity(localId, name.trim(), description?.trim(), category.name, scheduledTimeMinutes, encodeDays(repeatDays), encodeList(checklist), isActive, encodeList(completionDates.toList()), metadata)
    private fun SyncMetadata.toDomain() = RecordMetadata(remoteId, userId, createdAt, updatedAt, deletedAt, when (syncStatus) { SyncStatus.PENDING -> SyncState.Pending; SyncStatus.SYNCED -> SyncState.Synced; SyncStatus.FAILED -> SyncState.Failed }, lastSyncError, deviceId)
    private fun userId() = (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId
    private fun authFailure() = AppResult.Failure(IllegalStateException("Authenticated session is required"))
}

private fun encodeDays(days: Set<SelfCareDay>) = days.joinToString(",") { it.name }
private fun decodeDays(value: String) = value.split(',').mapNotNull { part -> SelfCareDay.entries.firstOrNull { it.name == part } }.toSet()
private fun encodeList(values: Collection<String>) = values.map(String::trim).filter(String::isNotEmpty).joinToString("\u001f")
private fun decodeList(value: String) = value.split("\u001f").map(String::trim).filter(String::isNotEmpty)
