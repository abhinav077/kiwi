package com.abhinavsirohi.kiwi.core.notifications

import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskReminderReconcilerTest {
    @Test
    fun reconciliation_reschedulesEveryRoomCandidate() = runBlocking {
        val candidates = listOf(task("task-1"), task("task-2"))
        val scheduler = RecordingReminderScheduler()
        val reconciler = TaskReminderReconciler(
            taskSource = ReminderTaskSource { candidates },
            reminderScheduler = scheduler,
        )

        val count = reconciler.reconcile()

        assertEquals(2, count)
        assertEquals(listOf("task-1", "task-2"), scheduler.scheduled.map(Task::localId))
    }

    private fun task(localId: String) = Task(
        localId = localId,
        title = "Private task",
        scheduledDate = "2026-07-15",
        scheduledTimeMinutes = 9 * 60,
        metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1"),
    )
}

private class RecordingReminderScheduler : TaskReminderScheduler {
    val scheduled = mutableListOf<Task>()

    override fun schedule(task: Task) {
        scheduled += task
    }

    override fun cancel(taskLocalId: String) = Unit
}
