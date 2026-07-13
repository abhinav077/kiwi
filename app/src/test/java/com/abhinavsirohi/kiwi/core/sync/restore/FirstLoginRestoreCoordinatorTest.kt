package com.abhinavsirohi.kiwi.core.sync.restore

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLoginRestoreCoordinatorTest {
    private val sessionProvider = FakeSessionProvider()
    private val remote = FakeRestoreRemoteDataSource()
    private val writer = FakeRestoreDatabaseWriter()
    private val reminders = FakeReminderRecreator()
    private val stateStore = FakeRestoreStateStore()

    @Test
    fun firstLogin_restoresThenRecreatesRemindersAndMarksCompleteLast() = runBlocking {
        val events = mutableListOf<String>()
        writer.events = events
        reminders.events = events
        stateStore.events = events

        val result = coordinator().restoreIfRequired()

        assertEquals(RestoreState.Completed(restoredTasks = 1, restoredSubtasks = 0), result)
        assertEquals(listOf("database", "reminders", "complete"), events)
        assertTrue(stateStore.completed)
    }

    @Test
    fun completedUser_isSkippedWithoutFetchingRemoteData() = runBlocking {
        stateStore.completed = true

        assertEquals(RestoreState.NotRequired, coordinator().restoreIfRequired())
        assertEquals(0, remote.fetchCount)
    }

    @Test
    fun networkFailure_isRetryableAndDoesNotMarkComplete() = runBlocking {
        remote.result = RemoteResult.Failure(RemoteError.NetworkUnavailable)

        assertEquals(
            RestoreState.RetryableFailure(RemoteError.NetworkUnavailable),
            coordinator().restoreIfRequired(),
        )
        assertFalse(stateStore.completed)
    }

    @Test
    fun crossUserSnapshot_isRejectedBeforeDatabaseWrite() = runBlocking {
        remote.result = RemoteResult.Success(snapshot(userId = "other-user"))

        assertEquals(
            RestoreState.Rejected(RemoteError.AccessDenied),
            coordinator().restoreIfRequired(),
        )
        assertEquals(0, writer.writeCount)
    }

    @Test
    fun reminderFailure_keepsRestoreRetryableAndIncomplete() = runBlocking {
        reminders.failure = IllegalStateException("scheduler unavailable")

        assertEquals(
            RestoreState.RetryableFailure(RemoteError.Unknown),
            coordinator().restoreIfRequired(),
        )
        assertFalse(stateStore.completed)
    }

    @Test
    fun unauthenticatedUser_isRejectedWithoutRemoteAccess() = runBlocking {
        sessionProvider.result = RemoteResult.Failure(RemoteError.AuthenticationRequired)

        assertEquals(
            RestoreState.Rejected(RemoteError.AuthenticationRequired),
            coordinator().restoreIfRequired(),
        )
        assertEquals(0, remote.fetchCount)
    }

    private fun coordinator() = FirstLoginRestoreCoordinator(
        sessionProvider = sessionProvider,
        remoteDataSource = remote,
        databaseWriter = writer,
        reminderRecreator = reminders,
        restoreStateStore = stateStore,
    )

    private fun snapshot(userId: String = USER_ID) = RestoreSnapshot(
        tasks = listOf(
            Task(
                localId = "task-1",
                title = "Restored task",
                metadata = RecordMetadata(
                    userId = userId,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    deviceId = "remote-device",
                ),
            ),
        ),
        subtasks = emptyList(),
    )

    private companion object {
        const val USER_ID = "user-1"
    }
}

private class FakeSessionProvider : SessionProvider {
    var result: RemoteResult<AuthenticatedSession> = RemoteResult.Success(
        AuthenticatedSession(userId = "user-1", email = "approved@example.com"),
    )

    override fun currentSession(): RemoteResult<AuthenticatedSession> = result
}

private class FakeRestoreRemoteDataSource : RestoreRemoteDataSource {
    var fetchCount = 0
    var result: RemoteResult<RestoreSnapshot> = RemoteResult.Success(
        RestoreSnapshot(
            tasks = listOf(
                Task(
                    localId = "task-1",
                    title = "Restored task",
                    metadata = RecordMetadata(
                        userId = "user-1",
                        createdAt = 1_000L,
                        updatedAt = 1_000L,
                        deviceId = "remote-device",
                    ),
                ),
            ),
            subtasks = emptyList(),
        ),
    )

    override suspend fun fetchSnapshot(userId: String): RemoteResult<RestoreSnapshot> {
        fetchCount += 1
        return result
    }
}

private class FakeRestoreDatabaseWriter : RestoreDatabaseWriter {
    var writeCount = 0
    var events: MutableList<String>? = null

    override suspend fun applySnapshot(snapshot: RestoreSnapshot): RestoreWriteResult {
        writeCount += 1
        events?.add("database")
        return RestoreWriteResult(snapshot.tasks.size, snapshot.subtasks.size)
    }
}

private class FakeReminderRecreator : ReminderRecreator {
    var failure: Throwable? = null
    var events: MutableList<String>? = null

    override suspend fun recreateAfterRestore(snapshot: RestoreSnapshot) {
        events?.add("reminders")
        failure?.let { throw it }
    }
}

private class FakeRestoreStateStore : RestoreStateStore {
    var completed = false
    var events: MutableList<String>? = null

    override suspend fun isComplete(userId: String): Boolean = completed

    override suspend fun markComplete(userId: String) {
        events?.add("complete")
        completed = true
    }
}
