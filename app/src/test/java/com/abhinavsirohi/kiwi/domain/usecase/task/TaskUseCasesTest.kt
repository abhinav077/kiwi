package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TaskUseCasesTest {
    private val repository = FakeTaskRepository()

    @Test
    fun observeUseCases_delegateToRepositoryContracts() = runBlocking {
        val task = task()
        val subtask = subtask()
        repository.tasksResult = AppResult.Success(listOf(task))
        repository.subtasksResult = AppResult.Success(listOf(subtask))

        assertEquals(repository.tasksResult, ObserveTasks(repository)().single())
        assertEquals(repository.subtasksResult, ObserveSubtasks(repository)(task.localId).single())
        assertEquals(task.localId, repository.observedSubtasksTaskId)
    }

    @Test
    fun saveUseCases_forwardUnchangedDomainModels() = runBlocking {
        val task = task()
        val subtask = subtask()

        assertEquals(AppResult.Success(Unit), SaveTask(repository)(task))
        assertEquals(AppResult.Success(Unit), SaveSubtask(repository)(subtask))
        assertSame(task, repository.savedTask)
        assertSame(subtask, repository.savedSubtask)
    }

    @Test
    fun tombstoneUseCases_forwardIdentifiersAndDeletionTime() = runBlocking {
        TombstoneTask(repository)(TombstoneTaskParameters(localId = "task-1", deletedAt = 2_000L))
        TombstoneSubtask(repository)(TombstoneSubtaskParameters(localId = "subtask-1", deletedAt = 3_000L))

        assertEquals("task-1" to 2_000L, repository.tombstonedTask)
        assertEquals("subtask-1" to 3_000L, repository.tombstonedSubtask)
    }

    @Test
    fun repositoryFailures_remainExplicit() = runBlocking {
        val failure = AppResult.Failure(IllegalStateException("database unavailable"))
        repository.saveTaskResult = failure

        assertSame(failure, SaveTask(repository)(task()))
    }

    private fun task() = Task(
        localId = "task-1",
        title = "Plan day",
        metadata = metadata(),
    )

    private fun subtask() = Subtask(
        localId = "subtask-1",
        taskLocalId = "task-1",
        title = "Pick priorities",
        metadata = metadata(),
    )

    private fun metadata() = RecordMetadata(
        userId = "user-1",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deviceId = "device-1",
    )
}

private class FakeTaskRepository : TaskRepository {
    var tasksResult: AppResult<List<Task>> = AppResult.Success(emptyList())
    var subtasksResult: AppResult<List<Subtask>> = AppResult.Success(emptyList())
    var observedSubtasksTaskId: String? = null
    var savedTask: Task? = null
    var savedSubtask: Subtask? = null
    var tombstonedTask: Pair<String, Long>? = null
    var tombstonedSubtask: Pair<String, Long>? = null
    var saveTaskResult: AppResult<Unit> = AppResult.Success(Unit)

    override fun observeTasks(): Flow<AppResult<List<Task>>> = flowOf(tasksResult)

    override fun observeSubtasks(taskLocalId: String): Flow<AppResult<List<Subtask>>> {
        observedSubtasksTaskId = taskLocalId
        return flowOf(subtasksResult)
    }

    override suspend fun saveTask(task: Task): AppResult<Unit> {
        savedTask = task
        return saveTaskResult
    }

    override suspend fun saveSubtask(subtask: Subtask): AppResult<Unit> {
        savedSubtask = subtask
        return AppResult.Success(Unit)
    }

    override suspend fun tombstoneTask(localId: String, deletedAt: Long): AppResult<Unit> {
        tombstonedTask = localId to deletedAt
        return AppResult.Success(Unit)
    }

    override suspend fun tombstoneSubtask(localId: String, deletedAt: Long): AppResult<Unit> {
        tombstonedSubtask = localId to deletedAt
        return AppResult.Success(Unit)
    }
}
