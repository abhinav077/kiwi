package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class TaskTitleRequiredException : IllegalArgumentException("Task title is required")

class CreateTask(
    private val taskRepository: TaskRepository,
) : UseCase<NewTask, AppResult<Task>> {
    override suspend fun invoke(parameters: NewTask): AppResult<Task> {
        val title = parameters.title.trim()
        if (title.isEmpty()) return AppResult.Failure(TaskTitleRequiredException())
        return taskRepository.createTask(parameters.copy(title = title))
    }
}
