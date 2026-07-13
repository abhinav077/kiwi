package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class ObserveTasks(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(): Flow<AppResult<List<Task>>> = taskRepository.observeTasks()
}
