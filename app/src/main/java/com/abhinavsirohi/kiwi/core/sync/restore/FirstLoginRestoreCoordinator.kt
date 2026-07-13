package com.abhinavsirohi.kiwi.core.sync.restore

import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider

class FirstLoginRestoreCoordinator(
    private val sessionProvider: SessionProvider,
    private val remoteDataSource: RestoreRemoteDataSource,
    private val databaseWriter: RestoreDatabaseWriter,
    private val reminderRecreator: ReminderRecreator,
    private val restoreStateStore: RestoreStateStore,
) {
    suspend fun restoreIfRequired(): RestoreState {
        val session = when (val result = sessionProvider.currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return result.error.toRestoreFailure()
        }
        if (restoreStateStore.isComplete(session.userId)) return RestoreState.NotRequired

        val snapshot = when (val result = remoteDataSource.fetchSnapshot(session.userId)) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return result.error.toRestoreFailure()
        }
        if (!snapshot.belongsTo(session.userId)) {
            return RestoreState.Rejected(RemoteError.AccessDenied)
        }

        return try {
            val result = databaseWriter.applySnapshot(snapshot)
            reminderRecreator.recreateAfterRestore(snapshot)
            restoreStateStore.markComplete(session.userId)
            RestoreState.Completed(result.restoredTasks, result.restoredSubtasks)
        } catch (_: Throwable) {
            RestoreState.RetryableFailure(RemoteError.Unknown)
        }
    }

    private fun RestoreSnapshot.belongsTo(userId: String): Boolean =
        tasks.all { it.metadata.userId == userId } &&
            subtasks.all { it.metadata.userId == userId }

    private fun RemoteError.toRestoreFailure(): RestoreState = when (this) {
        RemoteError.AuthenticationRequired,
        RemoteError.AccessDenied,
        RemoteError.ConfigurationMissing,
        RemoteError.ConfigurationInvalid,
        -> RestoreState.Rejected(this)
        RemoteError.NetworkUnavailable,
        RemoteError.ServiceUnavailable,
        RemoteError.Unknown,
        -> RestoreState.RetryableFailure(this)
    }
}
