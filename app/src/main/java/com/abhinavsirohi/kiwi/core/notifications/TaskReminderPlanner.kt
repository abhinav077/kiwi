package com.abhinavsirohi.kiwi.core.notifications

import com.abhinavsirohi.kiwi.domain.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ScheduledTaskReminder(
    val taskLocalId: String,
    val triggerAtMillis: Long,
)

data class ReminderNotificationContent(
    val title: String,
    val body: String,
)

class TaskReminderPlanner(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    fun reminderFor(task: Task): ScheduledTaskReminder? {
        if (task.isCompleted) return null
        val date = runCatching { LocalDate.parse(task.scheduledDate) }.getOrNull() ?: return null
        val minutes = task.scheduledTimeMinutes?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
        val triggerAtMillis = date
            .atTime(LocalTime.of(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return ScheduledTaskReminder(task.localId, triggerAtMillis).takeIf { it.triggerAtMillis > currentTimeMillis() }
    }

    fun notificationContent(): ReminderNotificationContent = ReminderNotificationContent(
        title = "Kiwi reminder",
        body = "You have a planned task.",
    )

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
