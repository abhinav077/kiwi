package com.abhinavsirohi.kiwi.domain.model

data class Task(
    val localId: String,
    val title: String,
    val description: String? = null,
    val category: TaskCategory = TaskCategory.Personal,
    val priority: TaskPriority = TaskPriority.Normal,
    val notes: String? = null,
    val scheduledDate: String? = null,
    val scheduledTimeMinutes: Int? = null,
    val recurrenceRule: RecurrenceRule = RecurrenceRule(),
    val recurrenceSeriesId: String? = null,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val metadata: RecordMetadata,
)

enum class TaskCategory {
    Personal,
    Work,
    Wellness,
    Home,
}

enum class TaskPriority {
    Low,
    Normal,
    High,
}

data class NewTask(
    val title: String,
    val description: String? = null,
    val category: TaskCategory = TaskCategory.Personal,
    val priority: TaskPriority = TaskPriority.Normal,
    val notes: String? = null,
    val scheduledDate: String? = null,
    val scheduledTimeMinutes: Int? = null,
    val recurrenceRule: RecurrenceRule = RecurrenceRule(),
)
