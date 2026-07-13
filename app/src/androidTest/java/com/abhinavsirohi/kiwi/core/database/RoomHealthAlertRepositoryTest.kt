package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.notifications.HealthAlertNotificationScheduler
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.RoomHealthAlertRepository
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.HealthAlertState
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHealthAlertRepositoryTest {
    private lateinit var database: KiwiDatabase
    private lateinit var repository: RoomHealthAlertRepository
    private val notifications = mutableListOf<HealthAlertEpisode>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            KiwiDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomHealthAlertRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() = RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            notifier = HealthAlertNotificationScheduler { notifications += it },
            today = { LocalDate.parse("2026-07-03") },
            now = { 2_000L },
        )
    }

    @After fun close() = database.close()

    @Test
    fun reconcile_deduplicatesNotifiesOnceAcknowledgesAndResolves() = runBlocking {
        val cycle = CycleRecord("cycle-1", "2026-07-01", metadata = metadata("cycle"))
        val daily = listOf(
            WellnessDailyRecord("d1", "2026-07-01", "cycle-1", painLevel = 7, metadata = metadata("d1")),
            WellnessDailyRecord("d2", "2026-07-02", "cycle-1", painLevel = 8, metadata = metadata("d2")),
        )
        assertTrue(repository.reconcile(listOf(cycle), daily) is AppResult.Success)
        assertTrue(repository.reconcile(listOf(cycle), daily) is AppResult.Success)
        assertEquals(1, notifications.size)
        assertEquals(1, database.healthAlertDao().getAll("user-1").size)
        val episode = (repository.observeVisibleEpisodes().first() as AppResult.Success).value.single()

        repository.acknowledge(episode.localId)
        assertEquals(HealthAlertState.Acknowledged, (repository.observeVisibleEpisodes().first() as AppResult.Success).value.single().state)

        repository.reconcile(listOf(cycle.copy(endDate = "2026-07-03")), daily)
        assertTrue((repository.observeVisibleEpisodes().first() as AppResult.Success).value.isEmpty())
        assertEquals("RESOLVED", database.healthAlertDao().find(episode.localId)?.state)
    }

    private fun metadata(id: String) = RecordMetadata(userId = "user-1", createdAt = 1, updatedAt = 1, deviceId = id)
}
