package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class SaveSubtask(
    private val taskRepository: TaskRepository,
) : UseCase<Subtask, AppResult<Unit>> {
    override suspend fun invoke(parameters: Subtask): AppResult<Unit> = taskRepository.saveSubtask(parameters)
}
