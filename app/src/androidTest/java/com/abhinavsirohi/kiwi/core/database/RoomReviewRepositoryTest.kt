package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.RoomReviewRepository
import com.abhinavsirohi.kiwi.domain.model.NewWeeklyReflection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomReviewRepositoryTest {
    private lateinit var database: KiwiDatabase
    private lateinit var repository: RoomReviewRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomReviewRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() =
                    RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            now = { now },
            newLocalId = { "reflection-1" },
        )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun saveReflection_createsThenUpdatesStableLocalFirstRecord() = runBlocking {
        val first = repository.saveReflection(NewWeeklyReflection("2026-07-20", "A steady week"))
        assertTrue(first is AppResult.Success)

        now = 2_000L
        val updated = repository.saveReflection(NewWeeklyReflection("2026-07-20", "A changed reflection"))
        assertTrue(updated is AppResult.Success)

        val records = (repository.observeReflections().first() as AppResult.Success).value
        assertEquals(1, records.size)
        assertEquals("reflection-1", records.single().localId)
        assertEquals("A changed reflection", records.single().content)
        assertEquals(
            SyncOperation.UPSERT,
            database.pendingChangeDao().findById("WEEKLY_REFLECTION:reflection-1")?.operation,
        )
        assertEquals(1, database.pendingChangeDao().count())
    }
}
