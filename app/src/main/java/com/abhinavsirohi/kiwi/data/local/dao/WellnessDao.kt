package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.CycleRecordEntity
import com.abhinavsirohi.kiwi.data.local.entity.WellnessDailyRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessDao {
    @Upsert
    suspend fun upsertCycleRecord(record: CycleRecordEntity)

    @Upsert
    suspend fun upsertDailyRecord(record: WellnessDailyRecordEntity)

    @Query("SELECT * FROM cycle_records WHERE localId = :localId")
    suspend fun findCycleRecord(localId: String): CycleRecordEntity?

    @Query("SELECT * FROM wellness_daily_records WHERE localId = :localId")
    suspend fun findDailyRecord(localId: String): WellnessDailyRecordEntity?

    @Query(
        "SELECT * FROM wellness_daily_records WHERE user_id = :userId AND record_date = :recordDate LIMIT 1",
    )
    suspend fun findDailyRecordByDate(userId: String, recordDate: String): WellnessDailyRecordEntity?

    @Query(
        "SELECT * FROM cycle_records WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY start_date DESC, created_at DESC",
    )
    fun observeActiveCycleRecords(userId: String): Flow<List<CycleRecordEntity>>

    @Query(
        "SELECT * FROM wellness_daily_records WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY record_date DESC, created_at DESC",
    )
    fun observeActiveDailyRecords(userId: String): Flow<List<WellnessDailyRecordEntity>>

    @Query("SELECT * FROM cycle_records WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getCycleRecordsAwaitingSync(): List<CycleRecordEntity>

    @Query("SELECT * FROM wellness_daily_records WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getDailyRecordsAwaitingSync(): List<WellnessDailyRecordEntity>
}
