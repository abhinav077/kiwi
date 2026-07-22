package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewWeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observePostponements(): Flow<AppResult<List<TaskPostponement>>>
    fun observeReflections(): Flow<AppResult<List<WeeklyReflection>>>
    suspend fun saveReflection(reflection: NewWeeklyReflection): AppResult<WeeklyReflection>
}
