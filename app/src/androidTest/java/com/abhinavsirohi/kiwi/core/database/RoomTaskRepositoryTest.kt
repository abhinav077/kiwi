package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.notifications.TaskReminderScheduler
import com.abhinavsirohi.kiwi.core.notifications.RoomReminderTaskSource
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.RoomTaskRepository
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.RecurrenceRule
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPriority
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
class RoomTaskRepositoryTest {
    private lateinit var database: KiwiDatabase
    private lateinit var repository: RoomTaskRepository
    private lateinit var reminders: RecordingReminderScheduler
    private var now = 2_000L
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminders = RecordingReminderScheduler()
        repository = RoomTaskRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() =
                    RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            currentTimeMillis = { now },
            newLocalId = { if (nextId++ == 0) "task-1" else "subtask-$nextId" },
            reminderScheduler = reminders,
        )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun createEditCompleteAndDelete_areLocalFirstAndQueueStableChange() = runBlocking {
        val created = repository.createTask(
            NewTask(
                title = "Plan day",
                description = "Choose the important work",
                category = TaskCategory.Work,
                priority = TaskPriority.High,
                scheduledDate = "2026-07-14",
                scheduledTimeMinutes = 570,
            ),
        ) as AppResult.Success

        val stored = database.taskDao().findTask("task-1")
        assertEquals("WORK", stored?.category)
        assertEquals(570, stored?.scheduledTimeMinutes)
        assertEquals(SyncStatus.PENDING, stored?.syncMetadata?.syncStatus)
        assertEquals(SyncOperation.UPSERT, database.pendingChangeDao().findById("TASK:task-1")?.operation)
        assertEquals(1, repository.observePlannerSyncState().first().pendingCount)

        now = 3_000L
        assertTrue(repository.saveTask(created.value.copy(title = "Plan gently", isCompleted = true)) is AppResult.Success)
        assertEquals("Plan gently", database.taskDao().findTask("task-1")?.title)
        assertEquals(true, database.taskDao().findTask("task-1")?.isCompleted)

        now = 4_000L
        assertTrue(repository.tombstoneTask("task-1", now) is AppResult.Success)
        assertTrue((repository.observeTasks().first() as AppResult.Success).value.isEmpty())
        assertNotNull(database.taskDao().findTask("task-1")?.syncMetadata?.deletedAt)
        assertEquals(SyncOperation.DELETE, database.pendingChangeDao().findById("TASK:task-1")?.operation)
        assertEquals(1, database.pendingChangeDao().count())
        assertEquals(1, repository.observePlannerSyncState().first().pendingCount)
    }

    @Test
    fun timedTaskWrites_scheduleRescheduleAndCancelLocalReminders() = runBlocking {
        val created = repository.createTask(
            NewTask(title = "Call home", scheduledDate = "2026-07-14", scheduledTimeMinutes = 9 * 60),
        ) as AppResult.Success
        assertEquals(listOf("task-1"), reminders.scheduled.map { it.localId })

        repository.saveTask(created.value.copy(scheduledTimeMinutes = 10 * 60))
        assertEquals(listOf(9 * 60, 10 * 60), reminders.scheduled.map { it.scheduledTimeMinutes })

        repository.tombstoneTask(created.value.localId, 3_000L)
        assertEquals(listOf("task-1"), reminders.cancelled)
    }

    @Test
    fun movingScheduledTaskLater_recordsOnePostponementTransactionally() = runBlocking {
        val created = repository.createTask(
            NewTask(title = "Dentist", scheduledDate = "2026-07-21"),
        ) as AppResult.Success

        now = 3_000L
        assertTrue(repository.saveTask(created.value.copy(scheduledDate = "2026-07-25")) is AppResult.Success)
        val events = database.reviewDao().observePostponements("user-1").first()

        assertEquals(1, events.size)
        assertEquals("Dentist", events.single().taskTitle)
        assertEquals("2026-07-21", events.single().previousDate)
        assertEquals("2026-07-25", events.single().newDate)
        assertEquals(
            SyncOperation.UPSERT,
            database.pendingChangeDao().findById("TASK_POSTPONEMENT:${events.single().localId}")?.operation,
        )

        now = 4_000L
        assertTrue(repository.saveTask(created.value.copy(scheduledDate = "2026-07-20")) is AppResult.Success)
        assertEquals(1, database.reviewDao().observePostponements("user-1").first().size)
    }

    @Test
    fun reminderReconciliationSource_returnsOnlyActiveTimedIncompleteTasks() = runBlocking {
        val timed = repository.createTask(
            NewTask(title = "Timed", scheduledDate = "2026-07-15", scheduledTimeMinutes = 9 * 60),
        ) as AppResult.Success
        repository.createTask(NewTask(title = "Untimed", scheduledDate = "2026-07-15"))
        val completed = repository.createTask(
            NewTask(title = "Completed", scheduledDate = "2026-07-15", scheduledTimeMinutes = 10 * 60),
        ) as AppResult.Success
        repository.saveTask(completed.value.copy(isCompleted = true))

        val candidates = RoomReminderTaskSource(database.taskDao()).reminderCandidates()

        assertEquals(listOf(timed.value.localId), candidates.map { it.localId })
    }

    @Test
    fun subtasks_supportCompletionOrderingAndTombstoneQueueing() = runBlocking {
        val task = repository.createTask(NewTask(title = "Plan day")) as AppResult.Success
        val first = repository.createSubtask(NewSubtask(task.value.localId, "Choose priorities")) as AppResult.Success
        val second = repository.createSubtask(NewSubtask(task.value.localId, "Start gently")) as AppResult.Success

        assertEquals(0, database.taskDao().findSubtask(first.value.localId)?.position)
        assertEquals(1, database.taskDao().findSubtask(second.value.localId)?.position)
        assertTrue(repository.saveSubtask(first.value.copy(isCompleted = true)) is AppResult.Success)
        assertTrue(database.taskDao().findSubtask(first.value.localId)?.isCompleted == true)

        now = 3_000L
        assertTrue(repository.moveSubtask(second.value.localId, -1) is AppResult.Success)
        assertEquals(second.value.localId, database.taskDao().getActiveSubtasks(task.value.localId, "user-1").first().localId)

        now = 4_000L
        assertTrue(repository.tombstoneSubtask(second.value.localId, now) is AppResult.Success)
        assertEquals(1, database.taskDao().getActiveSubtasks(task.value.localId, "user-1").size)
        assertEquals(SyncOperation.DELETE, database.pendingChangeDao().findById("SUBTASK:${second.value.localId}")?.operation)
        assertEquals(3, repository.observePlannerSyncState().first().pendingCount)
    }

    @Test
    fun completingRecurringTask_generatesOneNextOccurrenceAndCopiesSubtasks() = runBlocking {
        val task = repository.createTask(
            NewTask(
                title = "Daily reset",
                scheduledDate = "2026-07-14",
                recurrenceRule = RecurrenceRule(
                    frequency = RecurrenceFrequency.Daily,
                    interval = 1,
                    endDate = "2026-07-16",
                ),
            ),
        ) as AppResult.Success
        repository.createSubtask(NewSubtask(task.value.localId, "Put things away"))

        now = 3_000L
        assertTrue(repository.saveTask(task.value.copy(isCompleted = true)) is AppResult.Success)

        val next = database.taskDao().findOccurrence(task.value.localId, "2026-07-15", "user-1")
        assertNotNull(next)
        assertEquals("Daily reset", next?.title)
        assertEquals(false, next?.isCompleted)
        assertEquals("DAILY", next?.recurrenceFrequency)
        val copiedSubtasks = database.taskDao().getActiveSubtasks(next!!.localId, "user-1")
        assertEquals(listOf("Put things away"), copiedSubtasks.map { it.title })
        assertEquals(false, copiedSubtasks.single().isCompleted)
        assertEquals(SyncOperation.UPSERT, database.pendingChangeDao().findById("TASK:${next.localId}")?.operation)
        assertEquals(SyncOperation.UPSERT, database.pendingChangeDao().findById("SUBTASK:${copiedSubtasks.single().localId}")?.operation)

        assertTrue(repository.saveTask(task.value.copy(isCompleted = true)) is AppResult.Success)
        val occurrences = (repository.observeTasks().first() as AppResult.Success).value
            .filter { it.recurrenceSeriesId == task.value.localId && it.scheduledDate == "2026-07-15" }
        assertEquals(1, occurrences.size)
    }
}

private class RecordingReminderScheduler : TaskReminderScheduler {
    val scheduled = mutableListOf<com.abhinavsirohi.kiwi.domain.model.Task>()
    val cancelled = mutableListOf<String>()

    override fun schedule(task: com.abhinavsirohi.kiwi.domain.model.Task) {
        scheduled += task
    }

    override fun cancel(taskLocalId: String) {
        cancelled += taskLocalId
    }
}
