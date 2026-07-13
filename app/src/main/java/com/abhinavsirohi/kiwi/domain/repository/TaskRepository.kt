package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<AppResult<List<Task>>>

    fun observeSubtasks(taskLocalId: String): Flow<AppResult<List<Subtask>>>

    suspend fun saveTask(task: Task): AppResult<Unit>

    suspend fun saveSubtask(subtask: Subtask): AppResult<Unit>

    suspend fun tombstoneTask(localId: String, deletedAt: Long): AppResult<Unit>

    suspend fun tombstoneSubtask(localId: String, deletedAt: Long): AppResult<Unit>
}
