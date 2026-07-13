package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewCycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import kotlinx.coroutines.flow.Flow

interface WellnessRepository {
    fun observeCycleRecords(): Flow<AppResult<List<CycleRecord>>>

    fun observeDailyRecords(): Flow<AppResult<List<WellnessDailyRecord>>>

    suspend fun createCycleRecord(record: NewCycleRecord): AppResult<CycleRecord>

    suspend fun saveCycleRecord(record: CycleRecord): AppResult<Unit>

    suspend fun tombstoneCycleRecord(localId: String, deletedAt: Long): AppResult<Unit>

    suspend fun createDailyRecord(record: NewWellnessDailyRecord): AppResult<WellnessDailyRecord>

    suspend fun saveDailyRecord(record: WellnessDailyRecord): AppResult<Unit>

    suspend fun tombstoneDailyRecord(localId: String, deletedAt: Long): AppResult<Unit>
}
