package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfCareDao {
    @Upsert suspend fun upsert(routine: SelfCareRoutineEntity)

    @Query("SELECT * FROM self_care_routines WHERE localId = :localId")
    suspend fun find(localId: String): SelfCareRoutineEntity?

    @Query("SELECT * FROM self_care_routines WHERE user_id = :userId AND deleted_at IS NULL ORDER BY isActive DESC, scheduledTimeMinutes, created_at")
    fun observeActive(userId: String): Flow<List<SelfCareRoutineEntity>>

    @Query("SELECT * FROM self_care_routines WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getAwaitingSync(): List<SelfCareRoutineEntity>

    @Query("SELECT * FROM self_care_routines WHERE deleted_at IS NULL AND isActive = 1 AND scheduledTimeMinutes IS NOT NULL")
    suspend fun getReminderCandidates(): List<SelfCareRoutineEntity>
}
