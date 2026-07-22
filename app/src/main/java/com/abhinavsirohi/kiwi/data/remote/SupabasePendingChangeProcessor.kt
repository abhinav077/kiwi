package com.abhinavsirohi.kiwi.data.remote

import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.SyncProcessingResult
import com.abhinavsirohi.kiwi.core.sync.SyncProcessor
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.core.sync.SyncRetryPolicy
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

fun interface KiwiChangeGateway {
    suspend fun apply(recordType: SyncRecordType, payload: kotlinx.serialization.json.JsonObject)
}

class SupabaseKiwiChangeGateway(private val client: SupabaseClient) : KiwiChangeGateway {
    override suspend fun apply(recordType: SyncRecordType, payload: kotlinx.serialization.json.JsonObject) {
        client.postgrest.rpc(
            "apply_kiwi_change",
            ApplyKiwiChangeParameters(recordType.name, payload),
        )
    }
}

class SupabasePendingChangeProcessor(
    private val database: KiwiDatabase,
    private val clientResult: RemoteResult<SupabaseClient>,
    private val sessionProvider: SessionProvider = when (clientResult) {
        is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
        is RemoteResult.Failure -> object : SessionProvider {
            override fun currentSession(): RemoteResult<AuthenticatedSession> = RemoteResult.Failure(clientResult.error)
        }
    },
    private val gateway: KiwiChangeGateway? = (clientResult as? RemoteResult.Success)?.value?.let(::SupabaseKiwiChangeGateway),
    private val now: () -> Long = System::currentTimeMillis,
    private val retryPolicy: SyncRetryPolicy = SyncRetryPolicy(),
) : SyncProcessor {
    override suspend fun processPendingChanges(): SyncProcessingResult {
        val session = when (val result = sessionProvider.currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return failureFor(result.error)
        }
        val remoteGateway = gateway ?: return failureFor((clientResult as RemoteResult.Failure).error)
        val queue = database.pendingChangeDao()
        queue.recoverInterruptedChanges()
        var sawRetry = false
        var sawPermanentFailure = false

        while (true) {
            val batch = queue.claimEligibleGeneric(now(), BATCH_SIZE)
            if (batch.isEmpty()) break
            for (change in batch) {
                val record = findRecord(change)
                if (record == null) {
                    queue.markPermanentlyFailed(change.queueId, now(), "Local record is missing")
                    sawPermanentFailure = true
                    continue
                }
                try {
                    remoteGateway.apply(change.recordType, syncPayload(change.recordType, record, session.email))
                    markRecordSynced(change.recordType, record)
                    queue.markSuccessful(change.queueId)
                } catch (throwable: Throwable) {
                    val failedAt = now()
                    val error = throwable.message?.take(MAX_ERROR_LENGTH)
                    if (change.attemptCount + 1 >= MAX_ATTEMPTS) {
                        queue.markPermanentlyFailed(change.queueId, failedAt, error)
                        sawPermanentFailure = true
                    } else {
                        queue.markForRetry(
                            change.queueId,
                            failedAt,
                            retryPolicy.nextAttemptAt(failedAt, change.attemptCount + 1),
                            error,
                        )
                        sawRetry = true
                    }
                }
            }
        }
        return when {
            sawRetry -> SyncProcessingResult.Retry()
            sawPermanentFailure -> SyncProcessingResult.PermanentFailure()
            else -> SyncProcessingResult.QueueDrained
        }
    }

    private suspend fun findRecord(change: PendingChangeEntity): Any? = when (change.recordType) {
        SyncRecordType.TASK -> database.taskDao().findTask(change.recordLocalId)
        SyncRecordType.SUBTASK -> database.taskDao().findSubtask(change.recordLocalId)
        SyncRecordType.PROFILE -> database.profileDao().findByLocalId(change.recordLocalId)
        SyncRecordType.CYCLE_RECORD -> database.wellnessDao().findCycleRecord(change.recordLocalId)
        SyncRecordType.WELLNESS_DAILY_RECORD -> database.wellnessDao().findDailyRecord(change.recordLocalId)
        SyncRecordType.HEALTH_ALERT_EPISODE -> database.healthAlertDao().find(change.recordLocalId)
        SyncRecordType.DIARY_ENTRY -> database.diaryDao().find(change.recordLocalId)
        SyncRecordType.SELF_CARE_ROUTINE -> database.selfCareDao().find(change.recordLocalId)
        SyncRecordType.TASK_POSTPONEMENT -> database.reviewDao().findPostponement(change.recordLocalId)
        SyncRecordType.WEEKLY_REFLECTION -> database.reviewDao().findReflectionByLocalId(change.recordLocalId)
        SyncRecordType.DIARY_PHOTO -> null
    }

    private suspend fun markRecordSynced(type: SyncRecordType, record: Any) {
        fun com.abhinavsirohi.kiwi.core.database.SyncMetadata.synced() =
            copy(syncStatus = SyncStatus.SYNCED, lastSyncError = null)
        when (type) {
            SyncRecordType.TASK -> (record as com.abhinavsirohi.kiwi.data.local.entity.TaskEntity).let {
                database.taskDao().upsertTask(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.SUBTASK -> (record as com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity).let {
                database.taskDao().upsertSubtask(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.PROFILE -> (record as com.abhinavsirohi.kiwi.data.local.entity.ProfileEntity).let {
                database.profileDao().upsert(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.CYCLE_RECORD -> (record as com.abhinavsirohi.kiwi.data.local.entity.CycleRecordEntity).let {
                database.wellnessDao().upsertCycleRecord(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.WELLNESS_DAILY_RECORD -> (record as com.abhinavsirohi.kiwi.data.local.entity.WellnessDailyRecordEntity).let {
                database.wellnessDao().upsertDailyRecord(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.HEALTH_ALERT_EPISODE -> (record as com.abhinavsirohi.kiwi.data.local.entity.HealthAlertEpisodeEntity).let {
                database.healthAlertDao().upsert(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.DIARY_ENTRY -> (record as com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity).let {
                database.diaryDao().upsert(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.SELF_CARE_ROUTINE -> (record as com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity).let {
                database.selfCareDao().upsert(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.TASK_POSTPONEMENT -> (record as com.abhinavsirohi.kiwi.data.local.entity.TaskPostponementEntity).let {
                database.reviewDao().upsertPostponement(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.WEEKLY_REFLECTION -> (record as com.abhinavsirohi.kiwi.data.local.entity.WeeklyReflectionEntity).let {
                database.reviewDao().upsertReflection(it.copy(syncMetadata = it.syncMetadata.synced()))
            }
            SyncRecordType.DIARY_PHOTO -> Unit
        }
    }

    private fun failureFor(error: RemoteError): SyncProcessingResult = when (error) {
        RemoteError.ConfigurationMissing, RemoteError.ConfigurationInvalid -> SyncProcessingResult.PermanentFailure()
        else -> SyncProcessingResult.Retry()
    }

    private companion object {
        const val BATCH_SIZE = 25
        const val MAX_ATTEMPTS = 8
        const val MAX_ERROR_LENGTH = 500
    }
}
