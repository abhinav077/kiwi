package com.abhinavsirohi.kiwi.domain.model

import java.time.LocalDate

data class PlannerDaySummary(
    val date: LocalDate,
    val planned: Int,
    val completed: Int,
)

data class PlannerWeekSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val planned: Int,
    val completed: Int,
)

data class PlannerAnalytics(
    val plannedTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionPercentage: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val missedTasks: Int = 0,
    val categoryCounts: Map<TaskCategory, Int> = emptyMap(),
    val recentDays: List<PlannerDaySummary> = emptyList(),
    val weeklySummaries: List<PlannerWeekSummary> = emptyList(),
)
