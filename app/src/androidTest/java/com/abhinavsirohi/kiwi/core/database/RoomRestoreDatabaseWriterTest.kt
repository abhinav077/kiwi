package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreSnapshot
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.data.repository.RoomRestoreDatabaseWriter
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRestoreDatabaseWriterTest {
    private lateinit var database: KiwiDatabase
    private lateinit var writer: RoomRestoreDatabaseWriter

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        writer = RoomRestoreDatabaseWriter(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun duplicateSafeRestore_preservesNewerLocalAndAppliesNewerRemote() = runBlocking {
        database.taskDao().upsertTask(localTask(localId = "local-newer", updatedAt = 3_000L))
        database.taskDao().upsertTask(localTask(localId = "remote-newer", updatedAt = 1_000L))

        val result = writer.applySnapshot(
            RestoreSnapshot(
                tasks = listOf(
                    remoteTask(localId = "local-newer", title = "Older remote", updatedAt = 2_000L),
                    remoteTask(localId = "remote-newer", title = "Newer remote", updatedAt = 4_000L),
                ),
                subtasks = emptyList(),
            ),
        )

        assertEquals(1, result.restoredTasks)
        assertEquals("Local value", database.taskDao().findTask("local-newer")?.title)
        assertEquals("Newer remote", database.taskDao().findTask("remote-newer")?.title)
    }

    @Test
    fun restoredTombstone_remainsStoredButHiddenFromActiveQueries() = runBlocking {
        val tombstone = remoteTask(
            localId = "deleted-task",
            title = "Deleted",
            updatedAt = 2_000L,
            deletedAt = 2_000L,
        )

        writer.applySnapshot(RestoreSnapshot(tasks = listOf(tombstone), subtasks = emptyList()))

        assertNotNull(database.taskDao().findTask("deleted-task"))
        assertTrue(database.taskDao().observeActiveTasks().first().isEmpty())
    }

    private fun localTask(localId: String, updatedAt: Long) = TaskEntity(
        localId = localId,
        title = "Local value",
        syncMetadata = SyncMetadata(
            userId = USER_ID,
            createdAt = 1_000L,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.PENDING,
            deviceId = "local-device",
        ),
    )

    private fun remoteTask(
        localId: String,
        title: String,
        updatedAt: Long,
        deletedAt: Long? = null,
    ) = Task(
        localId = localId,
        title = title,
        metadata = RecordMetadata(
            remoteId = "remote-$localId",
            userId = USER_ID,
            createdAt = 1_000L,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncState = SyncState.Synced,
            deviceId = "remote-device",
        ),
    )

    private companion object {
        const val USER_ID = "user-1"
    }
}
