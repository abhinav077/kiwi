package com.abhinavsirohi.kiwi.domain.model

data class Task(
    val localId: String,
    val title: String,
    val notes: String? = null,
    val scheduledDate: String? = null,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val metadata: RecordMetadata,
)
