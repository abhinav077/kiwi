package com.abhinavsirohi.kiwi.core.notifications

import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Task
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderPlannerTest {
    private val planner = TaskReminderPlanner(
        zoneId = ZoneId.of("UTC"),
        currentTimeMillis = { 1_784_100_000_000L },
    )

    @Test
    fun timedFutureTask_schedulesAtItsLocalDateAndTime() {
        val reminder = planner.reminderFor(task(date = "2026-07-15", minutes = 9 * 60 + 30))

        assertEquals("task-1", reminder?.taskLocalId)
        assertEquals(1_784_107_800_000L, reminder?.triggerAtMillis)
    }

    @Test
    fun untimedCompletedAndPastTasks_doNotSchedule() {
        assertNull(planner.reminderFor(task(date = "2026-07-15", minutes = null)))
        assertNull(planner.reminderFor(task(date = "2026-07-15", minutes = 600, completed = true)))
        assertNull(planner.reminderFor(task(date = "2026-07-14", minutes = 600)))
    }

    @Test
    fun notificationContent_isGenericAndDoesNotContainTaskDetails() {
        val content = planner.notificationContent()

        assertEquals("Kiwi reminder", content.title)
        assertEquals("You have a planned task.", content.body)
    }

    private fun task(date: String, minutes: Int?, completed: Boolean = false) = Task(
        localId = "task-1",
        title = "Private task title",
        scheduledDate = date,
        scheduledTimeMinutes = minutes,
        isCompleted = completed,
        metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1"),
    )
}
