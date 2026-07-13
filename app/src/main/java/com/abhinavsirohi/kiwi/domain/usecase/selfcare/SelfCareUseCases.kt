package com.abhinavsirohi.kiwi.domain.usecase.selfcare

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.repository.SelfCareRepository
import kotlinx.coroutines.flow.Flow

class ObserveSelfCareRoutines(private val repository: SelfCareRepository) {
    operator fun invoke(): Flow<AppResult<List<SelfCareRoutine>>> = repository.observeRoutines()
}

class CreateSelfCareRoutine(private val repository: SelfCareRepository) {
    suspend operator fun invoke(routine: NewSelfCareRoutine): AppResult<SelfCareRoutine> {
        if (routine.name.trim().isEmpty()) return AppResult.Failure(IllegalArgumentException("Routine name is required"))
        if (routine.scheduledTimeMinutes != null && routine.scheduledTimeMinutes !in 0 until 24 * 60) {
            return AppResult.Failure(IllegalArgumentException("Routine time is invalid"))
        }
        return repository.createRoutine(routine.copy(
            name = routine.name.trim(),
            description = routine.description?.trim()?.takeIf(String::isNotEmpty),
            checklist = routine.checklist.map(String::trim).filter(String::isNotEmpty),
        ))
    }
}

class SaveSelfCareRoutine(private val repository: SelfCareRepository) {
    suspend operator fun invoke(routine: SelfCareRoutine): AppResult<Unit> = repository.saveRoutine(routine)
}

data class TombstoneSelfCareRoutineParameters(val localId: String, val deletedAt: Long)

class TombstoneSelfCareRoutine(private val repository: SelfCareRepository) {
    suspend operator fun invoke(parameters: TombstoneSelfCareRoutineParameters): AppResult<Unit> =
        repository.tombstoneRoutine(parameters.localId, parameters.deletedAt)
}
