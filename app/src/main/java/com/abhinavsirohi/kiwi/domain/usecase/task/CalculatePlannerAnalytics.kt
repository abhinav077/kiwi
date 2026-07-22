package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.domain.model.PlannerAnalytics
import com.abhinavsirohi.kiwi.domain.model.PlannerDaySummary
import com.abhinavsirohi.kiwi.domain.model.PlannerWeekSummary
import com.abhinavsirohi.kiwi.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class CalculatePlannerAnalytics {
    operator fun invoke(tasks: List<Task>, today: LocalDate = LocalDate.now()): PlannerAnalytics {
        val dated = tasks.mapNotNull { task ->
            task.scheduledDate?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() to task }
        }.filter { (date, _) -> date != null }
        val planned = dated.count { (date, _) -> !date!!.isAfter(today) }
        val completed = dated.count { (date, task) -> !date!!.isAfter(today) && task.isCompleted }
        val completedDates = dated
            .filter { (_, task) -> task.isCompleted }
            .map { (date, _) -> date!! }
            .toSet()
        val recentDays = (13 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayTasks = dated.filter { (taskDate, _) -> taskDate == date }
            PlannerDaySummary(date, dayTasks.size, dayTasks.count { (_, task) -> task.isCompleted })
        }
        val weeks = (3 downTo 0).map { offset ->
            val weekStart = today.minusWeeks(offset.toLong()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            val weekTasks = dated.filter { (date, _) -> date!! in weekStart..weekEnd }
            PlannerWeekSummary(weekStart, weekEnd, weekTasks.size, weekTasks.count { (_, task) -> task.isCompleted })
        }
        val streaks = streakLengths(completedDates)
        val currentStreak = if (today in completedDates) {
            var cursor = today
            var length = 0
            while (cursor in completedDates) {
                length++
                cursor = cursor.minusDays(1)
            }
            length
        } else 0
        return PlannerAnalytics(
            plannedTasks = planned,
            completedTasks = completed,
            completionPercentage = if (planned == 0) 0 else (completed * 100 / planned).coerceIn(0, 100),
            currentStreakDays = currentStreak,
            longestStreakDays = streaks.maxOrNull() ?: 0,
            missedTasks = dated.count { (date, task) -> date!! < today && !task.isCompleted },
            categoryCounts = tasks.groupingBy(Task::category).eachCount(),
            recentDays = recentDays,
            weeklySummaries = weeks,
        )
    }

    private fun streakLengths(dates: Set<LocalDate>): List<Int> {
        if (dates.isEmpty()) return emptyList()
        val sorted = dates.sorted()
        val result = mutableListOf<Int>()
        var length = 1
        sorted.zipWithNext().forEach { (current, next) ->
            if (next == current.plusDays(1)) length++ else { result += length; length = 1 }
        }
        result += length
        return result
    }
}
