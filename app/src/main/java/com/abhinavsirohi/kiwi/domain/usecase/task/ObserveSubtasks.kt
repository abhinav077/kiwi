package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class ObserveSubtasks(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(taskLocalId: String): Flow<AppResult<List<Subtask>>> =
        taskRepository.observeSubtasks(taskLocalId)
}
