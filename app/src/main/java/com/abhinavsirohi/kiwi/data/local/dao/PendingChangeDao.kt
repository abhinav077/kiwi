package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.core.sync.SyncQueueState
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingChangeDao {
    @Upsert
    suspend fun enqueue(change: PendingChangeEntity)

    @Query(
        "SELECT * FROM pending_changes " +
            "WHERE state = 'PENDING' AND next_attempt_at <= :now " +
            "ORDER BY updated_at LIMIT :limit",
    )
    suspend fun getEligible(now: Long, limit: Int): List<PendingChangeEntity>

    @Query(
        "SELECT * FROM pending_changes " +
            "WHERE state = 'PENDING' AND record_type != 'DIARY_PHOTO' AND next_attempt_at <= :now " +
            "ORDER BY updated_at LIMIT :limit",
    )
    suspend fun getEligibleGeneric(now: Long, limit: Int): List<PendingChangeEntity>

    @Query("UPDATE pending_changes SET state = 'PROCESSING' WHERE queue_id IN (:queueIds)")
    suspend fun markProcessing(queueIds: List<String>)

    @Transaction
    suspend fun claimEligible(now: Long, limit: Int): List<PendingChangeEntity> {
        val changes = getEligible(now, limit)
        if (changes.isNotEmpty()) {
            markProcessing(changes.map(PendingChangeEntity::queueId))
        }
        return changes.map { it.copy(state = SyncQueueState.PROCESSING) }
    }

    @Transaction
    suspend fun claimEligibleGeneric(now: Long, limit: Int): List<PendingChangeEntity> {
        val changes = getEligibleGeneric(now, limit)
        if (changes.isNotEmpty()) {
            markProcessing(changes.map(PendingChangeEntity::queueId))
        }
        return changes.map { it.copy(state = SyncQueueState.PROCESSING) }
    }

    @Query("DELETE FROM pending_changes WHERE queue_id = :queueId")
    suspend fun markSuccessful(queueId: String)

    @Query(
        "UPDATE pending_changes SET state = 'PENDING', attempt_count = attempt_count + 1, " +
            "next_attempt_at = :nextAttemptAt, updated_at = :failedAt, last_error = :error " +
            "WHERE queue_id = :queueId",
    )
    suspend fun markForRetry(queueId: String, failedAt: Long, nextAttemptAt: Long, error: String?)

    @Query(
        "UPDATE pending_changes SET state = 'FAILED', attempt_count = attempt_count + 1, " +
            "updated_at = :failedAt, last_error = :error WHERE queue_id = :queueId",
    )
    suspend fun markPermanentlyFailed(queueId: String, failedAt: Long, error: String?)

    @Query("UPDATE pending_changes SET state = 'PENDING' WHERE state = 'PROCESSING'")
    suspend fun recoverInterruptedChanges()

    @Query("SELECT * FROM pending_changes WHERE queue_id = :queueId")
    suspend fun findById(queueId: String): PendingChangeEntity?

    @Query("SELECT COUNT(*) FROM pending_changes")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM pending_changes WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_changes WHERE state = 'PENDING' AND record_type != 'DIARY_PHOTO'")
    fun observeGenericPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_changes WHERE state = 'PROCESSING'")
    fun observeProcessingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_changes WHERE state = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pending_changes " +
        "WHERE record_type IN ('TASK', 'SUBTASK', 'TASK_POSTPONEMENT') AND state = 'PENDING'",
    )
    fun observePlannerPendingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pending_changes " +
        "WHERE record_type IN ('TASK', 'SUBTASK', 'TASK_POSTPONEMENT') AND state = 'PROCESSING'",
    )
    fun observePlannerProcessingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pending_changes " +
        "WHERE record_type IN ('TASK', 'SUBTASK', 'TASK_POSTPONEMENT') AND state = 'FAILED'",
    )
    fun observePlannerFailedCount(): Flow<Int>
}
