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
import com.abhinavsirohi.kiwi.data.repository.RoomWellnessRepository
import com.abhinavsirohi.kiwi.domain.model.NewCycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
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
class RoomWellnessRepositoryTest {
    private lateinit var database: KiwiDatabase
    private lateinit var repository: RoomWellnessRepository
    private var now = 2_000L
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomWellnessRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() =
                    RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            currentTimeMillis = { now },
            newLocalId = { if (nextId++ == 0) "cycle-1" else "daily-$nextId" },
        )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun cycleRecord_createEditAndDelete_areLocalFirstAndQueued() = runBlocking {
        val created = repository.createCycleRecord(NewCycleRecord(startDate = "2026-07-10")) as AppResult.Success
        assertEquals("2026-07-10", database.wellnessDao().findCycleRecord("cycle-1")?.startDate)
        assertEquals(
            SyncOperation.UPSERT,
            database.pendingChangeDao().findById("CYCLE_RECORD:cycle-1")?.operation,
        )

        now = 3_000L
        assertTrue(repository.saveCycleRecord(created.value.copy(endDate = "2026-07-14")) is AppResult.Success)
        assertEquals("2026-07-14", database.wellnessDao().findCycleRecord("cycle-1")?.endDate)

        now = 4_000L
        assertTrue(repository.tombstoneCycleRecord("cycle-1", now) is AppResult.Success)
        assertTrue((repository.observeCycleRecords().first() as AppResult.Success).value.isEmpty())
        assertNotNull(database.wellnessDao().findCycleRecord("cycle-1")?.syncMetadata?.deletedAt)
        assertEquals(
            SyncOperation.DELETE,
            database.pendingChangeDao().findById("CYCLE_RECORD:cycle-1")?.operation,
        )
    }

    @Test
    fun dailyRecord_createEditDeleteAndRecreate_preservesOneRecordPerDate() = runBlocking {
        val cycle = repository.createCycleRecord(
            NewCycleRecord(startDate = "2026-07-10", endDate = "2026-07-14"),
        ) as AppResult.Success
        val daily = repository.createDailyRecord(
            NewWellnessDailyRecord(recordDate = "2026-07-12", cycleLocalId = cycle.value.localId),
        ) as AppResult.Success
        assertEquals(cycle.value.localId, daily.value.cycleLocalId)

        now = 3_000L
        assertTrue(repository.saveDailyRecord(daily.value.copy(cycleLocalId = null)) is AppResult.Success)
        assertEquals(null, database.wellnessDao().findDailyRecord(daily.value.localId)?.cycleLocalId)

        now = 4_000L
        assertTrue(repository.tombstoneDailyRecord(daily.value.localId, now) is AppResult.Success)
        assertTrue((repository.observeDailyRecords().first() as AppResult.Success).value.isEmpty())

        now = 5_000L
        val recreated = repository.createDailyRecord(
            NewWellnessDailyRecord(recordDate = "2026-07-12", cycleLocalId = cycle.value.localId),
        ) as AppResult.Success
        assertEquals(daily.value.localId, recreated.value.localId)
        assertEquals(null, recreated.value.metadata.deletedAt)
        assertEquals(1, (repository.observeDailyRecords().first() as AppResult.Success).value.size)
        assertEquals(
            SyncOperation.UPSERT,
            database.pendingChangeDao().findById("WELLNESS_DAILY_RECORD:${daily.value.localId}")?.operation,
        )
    }

    @Test
    fun invalidOrForeignRecords_areRejected() = runBlocking {
        assertTrue(
            repository.createCycleRecord(NewCycleRecord("2026-07-14", "2026-07-13")) is AppResult.Failure,
        )
        assertTrue(
            repository.createDailyRecord(NewWellnessDailyRecord("2026-07-14", "missing-cycle")) is AppResult.Failure,
        )
        assertEquals(0, database.pendingChangeDao().count())
    }

    @Test
    fun dailyWellnessFields_roundTripEditAndRemainInStableQueueRecord() = runBlocking {
        val created = repository.createDailyRecord(
            NewWellnessDailyRecord(
                recordDate = "2026-07-14",
                flow = WellnessFlow.Medium,
                painLevel = 4,
                crampsLevel = 5,
                symptoms = listOf("Fatigue", "Headache", "Fatigue"),
                mood = "Calm",
                energyLevel = 6,
                sleepMinutes = 450,
                notes = "Rested after lunch",
                exercise = "Walk",
                selfCareMedicationNotes = "Tea",
            ),
        ) as AppResult.Success

        val stored = database.wellnessDao().findDailyRecord(created.value.localId)
        assertEquals("MEDIUM", stored?.flow)
        assertEquals(4, stored?.painLevel)
        assertEquals(5, stored?.crampsLevel)
        assertEquals(6, stored?.energyLevel)
        assertEquals(450, stored?.sleepMinutes)
        assertEquals("Rested after lunch", stored?.notes)
        assertEquals(listOf("Fatigue", "Headache"), created.value.symptoms)

        now = 3_000L
        val edited = created.value.copy(
            flow = WellnessFlow.Heavy,
            painLevel = 8,
            symptoms = listOf("Nausea"),
            selfCareMedicationNotes = "Warm compress",
        )
        assertTrue(repository.saveDailyRecord(edited) is AppResult.Success)
        val observed = (repository.observeDailyRecords().first() as AppResult.Success).value.single()
        assertEquals(WellnessFlow.Heavy, observed.flow)
        assertEquals(8, observed.painLevel)
        assertEquals(listOf("Nausea"), observed.symptoms)
        assertEquals("Warm compress", observed.selfCareMedicationNotes)
        assertEquals(
            SyncOperation.UPSERT,
            database.pendingChangeDao().findById("WELLNESS_DAILY_RECORD:${created.value.localId}")?.operation,
        )
    }

    @Test
    fun outOfRangeDailyWellnessFields_areRejectedWithoutQueueChange() = runBlocking {
        val result = repository.createDailyRecord(
            NewWellnessDailyRecord(recordDate = "2026-07-14", energyLevel = -1),
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(0, database.pendingChangeDao().count())
    }
}
