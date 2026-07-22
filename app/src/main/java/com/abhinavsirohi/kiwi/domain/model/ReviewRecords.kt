package com.abhinavsirohi.kiwi.domain.model

data class TaskPostponement(
    val localId: String,
    val taskLocalId: String,
    val taskTitle: String,
    val previousDate: String,
    val newDate: String,
    val postponedAt: Long,
    val metadata: RecordMetadata,
)

data class WeeklyReflection(
    val localId: String,
    val weekStart: String,
    val content: String,
    val metadata: RecordMetadata,
)

data class NewWeeklyReflection(
    val weekStart: String,
    val content: String,
)
