package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncQueueState
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncQueueDatabaseTest {
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
    fun repeatedChange_coalescesAndKeepsLatestOperation() = runBlocking {
        val dao = database.pendingChangeDao()
        dao.enqueue(change(operation = SyncOperation.UPSERT, updatedAt = 1_000L))
        dao.enqueue(change(operation = SyncOperation.DELETE, updatedAt = 2_000L))

        assertEquals(1, dao.count())
        val stored = dao.findById(QUEUE_ID)
        assertNotNull(stored)
        assertEquals(SyncOperation.DELETE, stored?.operation)
        assertEquals(2_000L, stored?.updatedAt)
    }

    @Test
    fun transientAndPermanentFailures_neverDiscardQueuedChange() = runBlocking {
        val dao = database.pendingChangeDao()
        dao.enqueue(change())

        assertEquals(SyncQueueState.PROCESSING, dao.claimEligible(now = 1_000L, limit = 10).single().state)
        dao.markForRetry(QUEUE_ID, failedAt = 2_000L, nextAttemptAt = 4_000L, error = "offline")

        val retry = dao.findById(QUEUE_ID)
        assertEquals(SyncQueueState.PENDING, retry?.state)
        assertEquals(1, retry?.attemptCount)
        assertEquals("offline", retry?.lastError)
        assertEquals(1, dao.count())

        dao.markPermanentlyFailed(QUEUE_ID, failedAt = 5_000L, error = "rejected")
        val failed = dao.findById(QUEUE_ID)
        assertEquals(SyncQueueState.FAILED, failed?.state)
        assertEquals(2, failed?.attemptCount)
        assertEquals(1, dao.count())
    }

    @Test
    fun interruptedProcessing_isRecoveredAsPending() = runBlocking {
        val dao = database.pendingChangeDao()
        dao.enqueue(change())
        dao.claimEligible(now = 1_000L, limit = 1)

        dao.recoverInterruptedChanges()

        assertEquals(SyncQueueState.PENDING, dao.findById(QUEUE_ID)?.state)
    }

    private fun change(
        operation: SyncOperation = SyncOperation.UPSERT,
        updatedAt: Long = 1_000L,
    ) = PendingChangeEntity(
        queueId = QUEUE_ID,
        recordType = SyncRecordType.TASK,
        recordLocalId = "task-1",
        operation = operation,
        createdAt = 1_000L,
        updatedAt = updatedAt,
    )

    private companion object {
        const val QUEUE_ID = "TASK:task-1"
    }
}
