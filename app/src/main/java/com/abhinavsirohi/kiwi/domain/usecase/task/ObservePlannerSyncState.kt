package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class ObservePlannerSyncState(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(): Flow<PlannerSyncState> = taskRepository.observePlannerSyncState()
}
