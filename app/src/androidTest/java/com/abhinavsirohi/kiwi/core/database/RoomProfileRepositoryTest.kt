package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.RoomProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomProfileRepositoryTest {
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
    fun savePreferredName_writesProfileAndPendingChangeTransactionally() = runBlocking {
        val repository = RoomProfileRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() =
                    RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            currentTimeMillis = { 2_000L },
        )

        val result = repository.savePreferredName("Abhi")

        assertTrue(result is AppResult.Success)
        val stored = database.profileDao().findByUserId("user-1")
        assertEquals("Abhi", stored?.preferredName)
        assertEquals(SyncStatus.PENDING, stored?.syncMetadata?.syncStatus)
        val queued = database.pendingChangeDao().findById("PROFILE:user-1")
        assertEquals(SyncRecordType.PROFILE, queued?.recordType)
        assertEquals(SyncOperation.UPSERT, queued?.operation)
    }
}
