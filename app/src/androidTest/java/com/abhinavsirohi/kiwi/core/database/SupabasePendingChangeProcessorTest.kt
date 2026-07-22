package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncProcessingResult
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.KiwiChangeGateway
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabasePendingChangeProcessor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupabasePendingChangeProcessorTest {
    private lateinit var database: KiwiDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun successfulRemoteApply_marksLocalRecordSyncedAndRemovesQueueEntry() = runBlocking {
        val metadata = SyncMetadata(
            userId = "00000000-0000-0000-0000-000000000001",
            createdAt = 1_000L,
            updatedAt = 2_000L,
            deviceId = "device-1",
        )
        database.taskDao().upsertTask(TaskEntity(localId = "task-1", title = "Plan day", syncMetadata = metadata))
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "TASK:task-1",
                recordType = SyncRecordType.TASK,
                recordLocalId = "task-1",
                operation = SyncOperation.UPSERT,
                createdAt = 2_000L,
                updatedAt = 2_000L,
            ),
        )
        val gateway = RecordingGateway()
        val processor = SupabasePendingChangeProcessor(
            database = database,
            clientResult = RemoteResult.Failure(RemoteError.ConfigurationMissing),
            sessionProvider = object : SessionProvider {
                override fun currentSession() = RemoteResult.Success(
                    AuthenticatedSession(metadata.userId, "user@example.com"),
                )
            },
            gateway = gateway,
            now = { 3_000L },
        )

        assertEquals(SyncProcessingResult.QueueDrained, processor.processPendingChanges())
        assertEquals(SyncRecordType.TASK, gateway.recordType)
        assertEquals("task-1", gateway.payload?.get("local_id")?.jsonPrimitive?.content)
        assertEquals("Plan day", gateway.payload?.get("title")?.jsonPrimitive?.content)
        assertEquals(SyncStatus.SYNCED, database.taskDao().findTask("task-1")?.syncMetadata?.syncStatus)
        assertNull(database.pendingChangeDao().findById("TASK:task-1"))
    }
}

private class RecordingGateway : KiwiChangeGateway {
    var recordType: SyncRecordType? = null
    var payload: JsonObject? = null

    override suspend fun apply(recordType: SyncRecordType, payload: JsonObject) {
        this.recordType = recordType
        this.payload = payload
    }
}
