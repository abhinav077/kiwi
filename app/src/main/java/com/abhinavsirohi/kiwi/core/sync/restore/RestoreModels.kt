package com.abhinavsirohi.kiwi.core.sync.restore

import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task

data class RestoreSnapshot(
    val tasks: List<Task>,
    val subtasks: List<Subtask>,
)

sealed interface RestoreState {
    data object Restoring : RestoreState
    data object NotRequired : RestoreState
    data class Completed(val restoredTasks: Int, val restoredSubtasks: Int) : RestoreState
    data class RetryableFailure(val error: RemoteError) : RestoreState
    data class Rejected(val error: RemoteError) : RestoreState
}

fun interface RestoreRemoteDataSource {
    suspend fun fetchSnapshot(userId: String): RemoteResult<RestoreSnapshot>
}

fun interface RestoreDatabaseWriter {
    suspend fun applySnapshot(snapshot: RestoreSnapshot): RestoreWriteResult
}

data class RestoreWriteResult(
    val restoredTasks: Int,
    val restoredSubtasks: Int,
)

interface RestoreStateStore {
    suspend fun isComplete(userId: String): Boolean
    suspend fun markComplete(userId: String)
}

fun interface ReminderRecreator {
    suspend fun recreateAfterRestore(snapshot: RestoreSnapshot)
}
