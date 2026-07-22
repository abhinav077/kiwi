package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatePlannerAnalyticsTest {
    private val calculate = CalculatePlannerAnalytics()
    private val today = LocalDate.of(2026, 7, 21)

    @Test
    fun calculatesCompletionMissedCategoriesAndStreaksFromRecordedTasks() {
        val result = calculate(
            listOf(
                task("2026-07-19", true, TaskCategory.Work),
                task("2026-07-20", true, TaskCategory.Work),
                task("2026-07-21", true, TaskCategory.Personal),
                task("2026-07-18", false, TaskCategory.Home),
                task("2026-07-22", false, TaskCategory.Wellness),
                task("2026-07-23", true, TaskCategory.Personal),
            ),
            today,
        )

        assertEquals(4, result.plannedTasks)
        assertEquals(3, result.completedTasks)
        assertEquals(75, result.completionPercentage)
        assertEquals(3, result.currentStreakDays)
        assertEquals(3, result.longestStreakDays)
        assertEquals(1, result.missedTasks)
        assertEquals(2, result.categoryCounts[TaskCategory.Work])
        assertEquals(4, result.recentDays.sumOf { it.planned })
    }

    @Test
    fun emptyAndMalformedDatesRemainFactualAndSafe() {
        val result = calculate(listOf(task("not-a-date", false, TaskCategory.Personal)), today)

        assertEquals(0, result.plannedTasks)
        assertEquals(0, result.completionPercentage)
        assertEquals(0, result.missedTasks)
        assertEquals(4, result.weeklySummaries.size)
    }

    private fun task(date: String, completed: Boolean, category: TaskCategory) = Task(
        localId = date + category,
        title = "Task",
        scheduledDate = date,
        isCompleted = completed,
        category = category,
        metadata = RecordMetadata(userId = "user", createdAt = 0, updatedAt = 0, deviceId = "device"),
    )
}
