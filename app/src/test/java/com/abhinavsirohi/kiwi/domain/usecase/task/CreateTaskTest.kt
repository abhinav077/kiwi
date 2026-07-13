package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTaskTest {
    @Test
    fun blankTitle_isRejectedBeforeRepository() = runBlocking {
        val repository = RecordingRepository()

        val result = CreateTask(repository)(NewTask(title = "   "))

        assertTrue(result is AppResult.Failure && result.cause is TaskTitleRequiredException)
        assertEquals(null, repository.created)
    }

    @Test
    fun title_isTrimmedBeforeCreation() = runBlocking {
        val repository = RecordingRepository()

        CreateTask(repository)(NewTask(title = "  Plan day  "))

        assertEquals("Plan day", repository.created?.title)
    }
}

private class RecordingRepository : TaskRepository {
    var created: NewTask? = null

    override fun observeTasks(): Flow<AppResult<List<Task>>> = emptyFlow()
    override fun observeSubtasks(taskLocalId: String): Flow<AppResult<List<Subtask>>> = emptyFlow()
    override fun observePlannerSyncState(): Flow<PlannerSyncState> = emptyFlow()
    override suspend fun createTask(task: NewTask): AppResult<Task> {
        created = task
        return AppResult.Success(
            Task(
                localId = "task-1",
                title = task.title,
                metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1"),
            ),
        )
    }
    override suspend fun saveTask(task: Task): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun createSubtask(subtask: NewSubtask): AppResult<Subtask> =
        AppResult.Failure(UnsupportedOperationException())
    override suspend fun saveSubtask(subtask: Subtask): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun tombstoneTask(localId: String, deletedAt: Long): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun tombstoneSubtask(localId: String, deletedAt: Long): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun moveSubtask(localId: String, direction: Int): AppResult<Unit> = AppResult.Success(Unit)
}
