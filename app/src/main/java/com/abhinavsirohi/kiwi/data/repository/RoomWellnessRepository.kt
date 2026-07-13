package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.CycleRecordEntity
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.WellnessDailyRecordEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewCycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyFields
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import com.abhinavsirohi.kiwi.domain.model.validateCycleDates
import com.abhinavsirohi.kiwi.domain.model.validateWellnessDailyFields
import com.abhinavsirohi.kiwi.domain.model.validateWellnessRecordDate
import com.abhinavsirohi.kiwi.domain.repository.WellnessRepository
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomWellnessRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
) : WellnessRepository {
    private val json = Json

    constructor(
        database: KiwiDatabase,
        clientResult: RemoteResult<SupabaseClient>,
        deviceId: String,
    ) : this(database, sessionProvider(clientResult), deviceId)

    override fun observeCycleRecords(): Flow<AppResult<List<CycleRecord>>> {
        val userId = authenticatedUserId() ?: return authenticationFailureFlow()
        return database.wellnessDao().observeActiveCycleRecords(userId)
            .map<List<CycleRecordEntity>, AppResult<List<CycleRecord>>> { records ->
                AppResult.Success(records.map { record -> record.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override fun observeDailyRecords(): Flow<AppResult<List<WellnessDailyRecord>>> {
        val userId = authenticatedUserId() ?: return authenticationFailureFlow()
        return database.wellnessDao().observeActiveDailyRecords(userId)
            .map<List<WellnessDailyRecordEntity>, AppResult<List<WellnessDailyRecord>>> { records ->
                AppResult.Success(records.map { record -> record.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun createCycleRecord(record: NewCycleRecord): AppResult<CycleRecord> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        val dates = try {
            validateCycleDates(record.startDate, record.endDate)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        val now = currentTimeMillis()
        return persistCycleRecord(
            CycleRecordEntity(
                localId = newLocalId(),
                startDate = dates.first,
                endDate = dates.second,
                syncMetadata = SyncMetadata(
                    userId = userId,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                ),
            ),
            SyncOperation.UPSERT,
        )
    }

    override suspend fun saveCycleRecord(record: CycleRecord): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        val dates = try {
            validateCycleDates(record.startDate, record.endDate)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        return try {
            val existing = database.wellnessDao().findCycleRecord(record.localId)
                ?: return AppResult.Failure(IllegalArgumentException("Cycle record was not found"))
            if (existing.syncMetadata.userId != userId || record.metadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Cycle record belongs to another user"))
            }
            persistCycleRecord(
                existing.copy(
                    startDate = dates.first,
                    endDate = dates.second,
                    syncMetadata = existing.syncMetadata.pendingUpdate(currentTimeMillis()),
                ),
                SyncOperation.UPSERT,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun tombstoneCycleRecord(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        return try {
            val existing = database.wellnessDao().findCycleRecord(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Cycle record was not found"))
            if (existing.syncMetadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Cycle record belongs to another user"))
            }
            persistCycleRecord(
                existing.copy(syncMetadata = existing.syncMetadata.pendingDeletion(deletedAt)),
                SyncOperation.DELETE,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun createDailyRecord(record: NewWellnessDailyRecord): AppResult<WellnessDailyRecord> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        val recordDate = try {
            validateWellnessRecordDate(record.recordDate)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        return try {
            validateOwnedCycle(record.cycleLocalId, userId)?.let { return it }
            val now = currentTimeMillis()
            val existing = database.wellnessDao().findDailyRecordByDate(userId, recordDate)
            val fields = validateNewDailyFields(record)
            val entity = WellnessDailyRecordEntity(
                localId = existing?.localId ?: newLocalId(),
                recordDate = recordDate,
                cycleLocalId = record.cycleLocalId,
                flow = fields.flow?.name?.uppercase(),
                painLevel = fields.painLevel,
                crampsLevel = fields.crampsLevel,
                symptomsJson = fields.symptoms.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                mood = fields.mood,
                energyLevel = fields.energyLevel,
                sleepMinutes = fields.sleepMinutes,
                notes = fields.notes,
                exercise = fields.exercise,
                selfCareMedicationNotes = fields.selfCareMedicationNotes,
                syncMetadata = existing?.syncMetadata?.pendingUpdate(now) ?: SyncMetadata(
                    userId = userId,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                ),
            )
            persistDailyRecord(entity, SyncOperation.UPSERT)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun saveDailyRecord(record: WellnessDailyRecord): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        val recordDate = try {
            validateWellnessRecordDate(record.recordDate)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        return try {
            val existing = database.wellnessDao().findDailyRecord(record.localId)
                ?: return AppResult.Failure(IllegalArgumentException("Daily record was not found"))
            if (existing.syncMetadata.userId != userId || record.metadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Daily record belongs to another user"))
            }
            validateOwnedCycle(record.cycleLocalId, userId)?.let { return it }
            val fields = validateExistingDailyFields(record)
            persistDailyRecord(
                existing.copy(
                    recordDate = recordDate,
                    cycleLocalId = record.cycleLocalId,
                    flow = fields.flow?.name?.uppercase(),
                    painLevel = fields.painLevel,
                    crampsLevel = fields.crampsLevel,
                    symptomsJson = fields.symptoms.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                    mood = fields.mood,
                    energyLevel = fields.energyLevel,
                    sleepMinutes = fields.sleepMinutes,
                    notes = fields.notes,
                    exercise = fields.exercise,
                    selfCareMedicationNotes = fields.selfCareMedicationNotes,
                    syncMetadata = existing.syncMetadata.pendingUpdate(currentTimeMillis()),
                ),
                SyncOperation.UPSERT,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun tombstoneDailyRecord(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = authenticatedUserId() ?: return authenticationFailure()
        return try {
            val existing = database.wellnessDao().findDailyRecord(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Daily record was not found"))
            if (existing.syncMetadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Daily record belongs to another user"))
            }
            persistDailyRecord(
                existing.copy(syncMetadata = existing.syncMetadata.pendingDeletion(deletedAt)),
                SyncOperation.DELETE,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private suspend fun persistCycleRecord(
        entity: CycleRecordEntity,
        operation: SyncOperation,
    ): AppResult<CycleRecord> = try {
        database.withTransaction {
            database.wellnessDao().upsertCycleRecord(entity)
            enqueue(
                recordType = SyncRecordType.CYCLE_RECORD,
                localId = entity.localId,
                createdAt = entity.syncMetadata.createdAt,
                updatedAt = entity.syncMetadata.updatedAt,
                operation = operation,
            )
        }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable)
    }

    private suspend fun persistDailyRecord(
        entity: WellnessDailyRecordEntity,
        operation: SyncOperation,
    ): AppResult<WellnessDailyRecord> = try {
        database.withTransaction {
            database.wellnessDao().upsertDailyRecord(entity)
            enqueue(
                recordType = SyncRecordType.WELLNESS_DAILY_RECORD,
                localId = entity.localId,
                createdAt = entity.syncMetadata.createdAt,
                updatedAt = entity.syncMetadata.updatedAt,
                operation = operation,
            )
        }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable)
    }

    private suspend fun enqueue(
        recordType: SyncRecordType,
        localId: String,
        createdAt: Long,
        updatedAt: Long,
        operation: SyncOperation,
    ) {
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "$recordType:$localId",
                recordType = recordType,
                recordLocalId = localId,
                operation = operation,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ),
        )
    }

    private suspend fun validateOwnedCycle(localId: String?, userId: String): AppResult.Failure? {
        if (localId == null) return null
        val cycle = database.wellnessDao().findCycleRecord(localId)
            ?: return AppResult.Failure(IllegalArgumentException("Cycle record was not found"))
        return if (cycle.syncMetadata.userId != userId || cycle.syncMetadata.deletedAt != null) {
            AppResult.Failure(IllegalAccessException("Cycle record is unavailable"))
        } else {
            null
        }
    }

    private fun authenticatedUserId(): String? =
        (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId

    private fun SyncMetadata.pendingUpdate(updatedAt: Long) = copy(
        updatedAt = updatedAt,
        deletedAt = null,
        syncStatus = SyncStatus.PENDING,
        lastSyncError = null,
        deviceId = deviceId,
    )

    private fun SyncMetadata.pendingDeletion(deletedAt: Long) = copy(
        updatedAt = deletedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.PENDING,
        lastSyncError = null,
        deviceId = deviceId,
    )

    private fun CycleRecordEntity.toDomain() = CycleRecord(
        localId = localId,
        startDate = startDate,
        endDate = endDate,
        metadata = syncMetadata.toDomain(),
    )

    private fun WellnessDailyRecordEntity.toDomain() = WellnessDailyRecord(
        localId = localId,
        recordDate = recordDate,
        cycleLocalId = cycleLocalId,
        flow = WellnessFlow.entries.firstOrNull { it.name.equals(flow, ignoreCase = true) },
        painLevel = painLevel,
        crampsLevel = crampsLevel,
        symptoms = symptomsJson?.let(::decodeSymptoms).orEmpty(),
        mood = mood,
        energyLevel = energyLevel,
        sleepMinutes = sleepMinutes,
        notes = notes,
        exercise = exercise,
        selfCareMedicationNotes = selfCareMedicationNotes,
        metadata = syncMetadata.toDomain(),
    )

    private fun validateNewDailyFields(record: NewWellnessDailyRecord): WellnessDailyFields =
        validateWellnessDailyFields(
            flow = record.flow,
            painLevel = record.painLevel,
            crampsLevel = record.crampsLevel,
            symptoms = record.symptoms,
            mood = record.mood,
            energyLevel = record.energyLevel,
            sleepMinutes = record.sleepMinutes,
            notes = record.notes,
            exercise = record.exercise,
            selfCareMedicationNotes = record.selfCareMedicationNotes,
        )

    private fun validateExistingDailyFields(record: WellnessDailyRecord): WellnessDailyFields =
        validateWellnessDailyFields(
            flow = record.flow,
            painLevel = record.painLevel,
            crampsLevel = record.crampsLevel,
            symptoms = record.symptoms,
            mood = record.mood,
            energyLevel = record.energyLevel,
            sleepMinutes = record.sleepMinutes,
            notes = record.notes,
            exercise = record.exercise,
            selfCareMedicationNotes = record.selfCareMedicationNotes,
        )

    private fun decodeSymptoms(value: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(value)
    }.getOrDefault(emptyList())

    private fun SyncMetadata.toDomain() = RecordMetadata(
        remoteId = remoteId,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = when (syncStatus) {
            SyncStatus.PENDING -> SyncState.Pending
            SyncStatus.SYNCED -> SyncState.Synced
            SyncStatus.FAILED -> SyncState.Failed
        },
        lastSyncError = lastSyncError,
        deviceId = deviceId,
    )

    private fun <T> authenticationFailureFlow(): Flow<AppResult<T>> = flowOf(authenticationFailure())

    private fun authenticationFailure() =
        AppResult.Failure(IllegalStateException("Authenticated session is required"))

    private fun <T> AppResult<T>.asUnit(): AppResult<Unit> = when (this) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> this
    }

    private companion object {
        fun sessionProvider(clientResult: RemoteResult<SupabaseClient>): SessionProvider =
            when (clientResult) {
                is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
                is RemoteResult.Failure -> object : SessionProvider {
                    override fun currentSession() = clientResult
                }
            }
    }
}
