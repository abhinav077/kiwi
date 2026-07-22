package com.abhinavsirohi.kiwi.domain.usecase.review

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewWeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.repository.ReviewRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ObserveTaskPostponements(private val repository: ReviewRepository) {
    operator fun invoke(): Flow<AppResult<List<TaskPostponement>>> = repository.observePostponements()
}

class ObserveWeeklyReflections(private val repository: ReviewRepository) {
    operator fun invoke(): Flow<AppResult<List<WeeklyReflection>>> = repository.observeReflections()
}

class SaveWeeklyReflection(private val repository: ReviewRepository) {
    suspend operator fun invoke(reflection: NewWeeklyReflection): AppResult<WeeklyReflection> {
        if (reflection.content.isBlank()) {
            return AppResult.Failure(IllegalArgumentException("Reflection is required"))
        }
        if (runCatching { LocalDate.parse(reflection.weekStart) }.isFailure) {
            return AppResult.Failure(IllegalArgumentException("Reflection week is invalid"))
        }
        return repository.saveReflection(reflection.copy(content = reflection.content.trim()))
    }
}
