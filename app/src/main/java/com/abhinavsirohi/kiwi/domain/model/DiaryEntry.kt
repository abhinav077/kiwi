package com.abhinavsirohi.kiwi.domain.model

data class DiaryEntry(
    val localId: String,
    val title: String,
    val content: String,
    val entryDate: String,
    val bestThing: String? = null,
    val mood: String? = null,
    val isFavourite: Boolean = false,
    val metadata: RecordMetadata,
)

data class NewDiaryEntry(
    val title: String,
    val content: String,
    val entryDate: String,
    val bestThing: String? = null,
    val mood: String? = null,
    val isFavourite: Boolean = false,
)
