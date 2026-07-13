package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

data class MoveSubtaskParameters(
    val localId: String,
    val direction: Int,
)

class MoveSubtask(
    private val taskRepository: TaskRepository,
) : UseCase<MoveSubtaskParameters, AppResult<Unit>> {
    override suspend fun invoke(parameters: MoveSubtaskParameters): AppResult<Unit> =
        taskRepository.moveSubtask(parameters.localId, parameters.direction)
}
