package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import kotlinx.coroutines.flow.Flow

interface SelfCareRepository {
    fun observeRoutines(): Flow<AppResult<List<SelfCareRoutine>>>
    suspend fun createRoutine(routine: NewSelfCareRoutine): AppResult<SelfCareRoutine>
    suspend fun saveRoutine(routine: SelfCareRoutine): AppResult<Unit>
    suspend fun tombstoneRoutine(localId: String, deletedAt: Long): AppResult<Unit>
}
