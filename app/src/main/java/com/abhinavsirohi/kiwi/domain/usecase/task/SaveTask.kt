package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class SaveTask(
    private val taskRepository: TaskRepository,
) : UseCase<Task, AppResult<Unit>> {
    override suspend fun invoke(parameters: Task): AppResult<Unit> = taskRepository.saveTask(parameters)
}
