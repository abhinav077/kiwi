package com.abhinavsirohi.kiwi.domain.model

data class Subtask(
    val localId: String,
    val taskLocalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val metadata: RecordMetadata,
)

data class NewSubtask(
    val taskLocalId: String,
    val title: String,
)

fun calculateSubtaskProgress(subtasks: List<Subtask>): Float =
    if (subtasks.isEmpty()) 0f else subtasks.count(Subtask::isCompleted).toFloat() / subtasks.size
