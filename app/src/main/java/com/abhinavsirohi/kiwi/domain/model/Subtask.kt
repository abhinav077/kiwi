package com.abhinavsirohi.kiwi.domain.model

data class Subtask(
    val localId: String,
    val taskLocalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val metadata: RecordMetadata,
)
