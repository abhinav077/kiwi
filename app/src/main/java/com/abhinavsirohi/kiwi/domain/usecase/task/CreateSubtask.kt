package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class SubtaskTitleRequiredException : IllegalArgumentException("Subtask title is required")

class CreateSubtask(
    private val taskRepository: TaskRepository,
) : UseCase<NewSubtask, AppResult<Subtask>> {
    override suspend fun invoke(parameters: NewSubtask): AppResult<Subtask> {
        val title = parameters.title.trim()
        if (title.isEmpty()) return AppResult.Failure(SubtaskTitleRequiredException())
        return taskRepository.createSubtask(parameters.copy(title = title))
    }
}
