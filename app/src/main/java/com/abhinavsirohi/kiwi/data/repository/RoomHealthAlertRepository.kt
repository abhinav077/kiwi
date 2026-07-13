package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.notifications.HealthAlertNotificationScheduler
import com.abhinavsirohi.kiwi.core.notifications.NoOpHealthAlertNotificationScheduler
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.HealthAlertEpisodeEntity
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.HealthAlertState
import com.abhinavsirohi.kiwi.domain.model.HealthPatternType
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.repository.HealthAlertRepository
import com.abhinavsirohi.kiwi.domain.usecase.wellness.DetectHealthPatterns
import io.github.jan.supabase.SupabaseClient
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomHealthAlertRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val notifier: HealthAlertNotificationScheduler = NoOpHealthAlertNotificationScheduler,
    private val detector: DetectHealthPatterns = DetectHealthPatterns(),
    private val today: () -> LocalDate = LocalDate::now,
    private val now: () -> Long = System::currentTimeMillis,
) : HealthAlertRepository {
    constructor(
        database: KiwiDatabase,
        client: RemoteResult<SupabaseClient>,
        deviceId: String,
        notifier: HealthAlertNotificationScheduler = NoOpHealthAlertNotificationScheduler,
    ) : this(database, sessionProvider(client), deviceId, notifier)

    override fun observeVisibleEpisodes(): Flow<AppResult<List<HealthAlertEpisode>>> {
        val userId = userId() ?: return flowOf(authFailure())
        return database.healthAlertDao().observeVisible(userId)
            .map<List<HealthAlertEpisodeEntity>, AppResult<List<HealthAlertEpisode>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }.catch { emit(AppResult.Failure(it)) }
    }

    override suspend fun reconcile(
        cycles: List<CycleRecord>,
        dailyRecords: List<WellnessDailyRecord>,
    ): AppResult<Unit> {
        val userId = userId() ?: return authFailure()
        return try {
            val detections = detector(cycles, dailyRecords, today())
            val detectionIds = detections.map { episodeId(it.patternType, it.sourceCycleId) }.toSet()
            val notifications = mutableListOf<HealthAlertEpisode>()
            database.withTransaction {
                val existing = database.healthAlertDao().getAll(userId).associateBy { it.localId }
                detections.forEach { detection ->
                    val id = episodeId(detection.patternType, detection.sourceCycleId)
                    val previous = existing[id]
                    if (previous?.state in listOf(HealthAlertState.Dismissed.name.uppercase(), HealthAlertState.Resolved.name.uppercase())) {
                        return@forEach
                    }
                    val timestamp = now()
                    val entity = HealthAlertEpisodeEntity(
                        localId = id,
                        patternType = detection.patternType.name.uppercase(),
                        sourceCycleId = detection.sourceCycleId,
                        evidenceCount = detection.evidenceCount,
                        firstEvidenceDate = detection.firstEvidenceDate,
                        lastEvidenceDate = detection.lastEvidenceDate,
                        state = previous?.state ?: HealthAlertState.Active.name.uppercase(),
                        notifiedAt = previous?.notifiedAt ?: timestamp,
                        syncMetadata = previous?.syncMetadata?.pending(timestamp) ?: SyncMetadata(
                            userId = userId,
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            deviceId = deviceId,
                        ),
                    )
                    database.healthAlertDao().upsert(entity)
                    enqueue(entity)
                    if (previous == null) notifications += entity.toDomain()
                }
                existing.values.filter {
                    it.localId !in detectionIds && it.state in listOf(
                        HealthAlertState.Active.name.uppercase(),
                        HealthAlertState.Acknowledged.name.uppercase(),
                    )
                }.forEach { episode ->
                    val updated = episode.copy(
                        state = HealthAlertState.Resolved.name.uppercase(),
                        syncMetadata = episode.syncMetadata.pending(now()),
                    )
                    database.healthAlertDao().upsert(updated)
                    enqueue(updated)
                }
            }
            notifications.forEach { runCatching { notifier.schedule(it) } }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun acknowledge(localId: String): AppResult<Unit> = updateState(localId, HealthAlertState.Acknowledged)
    override suspend fun dismiss(localId: String): AppResult<Unit> = updateState(localId, HealthAlertState.Dismissed)

    private suspend fun updateState(localId: String, state: HealthAlertState): AppResult<Unit> {
        val userId = userId() ?: return authFailure()
        return try {
            val existing = database.healthAlertDao().find(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Health alert was not found"))
            if (existing.syncMetadata.userId != userId) return AppResult.Failure(IllegalAccessException("Health alert is unavailable"))
            val updated = existing.copy(state = state.name.uppercase(), syncMetadata = existing.syncMetadata.pending(now()))
            database.withTransaction {
                database.healthAlertDao().upsert(updated)
                enqueue(updated)
            }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private suspend fun enqueue(entity: HealthAlertEpisodeEntity) {
        database.pendingChangeDao().enqueue(PendingChangeEntity(
            queueId = "HEALTH_ALERT_EPISODE:${entity.localId}",
            recordType = SyncRecordType.HEALTH_ALERT_EPISODE,
            recordLocalId = entity.localId,
            operation = SyncOperation.UPSERT,
            createdAt = entity.syncMetadata.createdAt,
            updatedAt = entity.syncMetadata.updatedAt,
        ))
    }

    private fun HealthAlertEpisodeEntity.toDomain() = HealthAlertEpisode(
        localId, patternType.toPattern(), sourceCycleId, evidenceCount, firstEvidenceDate, lastEvidenceDate,
        state.toState(), notifiedAt, syncMetadata.toDomain(),
    )
    private fun String.toPattern() = HealthPatternType.entries.first { it.name.equals(this, true) }
    private fun String.toState() = HealthAlertState.entries.first { it.name.equals(this, true) }
    private fun SyncMetadata.pending(timestamp: Long) = copy(
        updatedAt = timestamp, syncStatus = SyncStatus.PENDING, lastSyncError = null, deviceId = deviceId,
    )
    private fun SyncMetadata.toDomain() = RecordMetadata(
        remoteId, userId, createdAt, updatedAt, deletedAt,
        when (syncStatus) { SyncStatus.PENDING -> SyncState.Pending; SyncStatus.SYNCED -> SyncState.Synced; SyncStatus.FAILED -> SyncState.Failed },
        lastSyncError, deviceId,
    )
    private fun userId() = (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId
    private fun authFailure() = AppResult.Failure(IllegalStateException("Authenticated session is required"))
    private fun episodeId(type: HealthPatternType, cycleId: String) = "${type.name.uppercase()}:$cycleId"

    private companion object {
        fun sessionProvider(client: RemoteResult<SupabaseClient>): SessionProvider = when (client) {
            is RemoteResult.Success -> SupabaseSessionProvider(client.value)
            is RemoteResult.Failure -> object : SessionProvider { override fun currentSession() = client }
        }
    }
}
