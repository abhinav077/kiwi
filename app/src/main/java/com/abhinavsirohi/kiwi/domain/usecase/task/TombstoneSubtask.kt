package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

data class TombstoneSubtaskParameters(
    val localId: String,
    val deletedAt: Long,
)

class TombstoneSubtask(
    private val taskRepository: TaskRepository,
) : UseCase<TombstoneSubtaskParameters, AppResult<Unit>> {
    override suspend fun invoke(parameters: TombstoneSubtaskParameters): AppResult<Unit> =
        taskRepository.tombstoneSubtask(parameters.localId, parameters.deletedAt)
}
