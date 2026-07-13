package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import kotlinx.coroutines.flow.Flow

interface HealthAlertRepository {
    fun observeVisibleEpisodes(): Flow<AppResult<List<HealthAlertEpisode>>>
    suspend fun reconcile(cycles: List<CycleRecord>, dailyRecords: List<WellnessDailyRecord>): AppResult<Unit>
    suspend fun acknowledge(localId: String): AppResult<Unit>
    suspend fun dismiss(localId: String): AppResult<Unit>
}
