package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiwiDatabaseTest {
    private lateinit var database: KiwiDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun taskAndSubtask_roundTripWithSyncMetadata() = runBlocking {
        val metadata = metadata(syncStatus = SyncStatus.FAILED, lastSyncError = "offline")
        val task = TaskEntity(localId = "task-1", title = "Plan day", syncMetadata = metadata)
        val subtask = SubtaskEntity(
            localId = "subtask-1",
            taskLocalId = task.localId,
            title = "Pick priorities",
            syncMetadata = metadata,
        )

        database.taskDao().upsertTask(task)
        database.taskDao().upsertSubtask(subtask)

        assertEquals(task, database.taskDao().observeActiveTasks().first().single())
        assertEquals(subtask, database.taskDao().observeActiveSubtasks(task.localId).first().single())
        assertEquals(listOf(task), database.taskDao().getTasksAwaitingSync())
        assertEquals(listOf(subtask), database.taskDao().getSubtasksAwaitingSync())
    }

    @Test
    fun tombstonedRecords_areHiddenButRemainAwaitingSync() = runBlocking {
        val task = TaskEntity(
            localId = "task-deleted",
            title = "Old task",
            syncMetadata = metadata(deletedAt = 2_000L),
        )

        database.taskDao().upsertTask(task)

        assertTrue(database.taskDao().observeActiveTasks().first().isEmpty())
        assertEquals(listOf(task), database.taskDao().getTasksAwaitingSync())
    }

    private fun metadata(
        deletedAt: Long? = null,
        syncStatus: SyncStatus = SyncStatus.PENDING,
        lastSyncError: String? = null,
    ) = SyncMetadata(
        remoteId = null,
        userId = "user-1",
        createdAt = 1_000L,
        updatedAt = 2_000L,
        deletedAt = deletedAt,
        syncStatus = syncStatus,
        lastSyncError = lastSyncError,
        deviceId = "device-1",
    )
}
