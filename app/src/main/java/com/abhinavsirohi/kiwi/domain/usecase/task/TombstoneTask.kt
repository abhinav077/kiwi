package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

data class TombstoneTaskParameters(
    val localId: String,
    val deletedAt: Long,
)

class TombstoneTask(
    private val taskRepository: TaskRepository,
) : UseCase<TombstoneTaskParameters, AppResult<Unit>> {
    override suspend fun invoke(parameters: TombstoneTaskParameters): AppResult<Unit> =
        taskRepository.tombstoneTask(parameters.localId, parameters.deletedAt)
}
