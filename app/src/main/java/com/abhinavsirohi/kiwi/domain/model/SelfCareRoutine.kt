package com.abhinavsirohi.kiwi.domain.model

data class SelfCareRoutine(
    val localId: String,
    val name: String,
    val description: String? = null,
    val category: SelfCareCategory = SelfCareCategory.Mind,
    val scheduledTimeMinutes: Int? = null,
    val repeatDays: Set<SelfCareDay> = emptySet(),
    val checklist: List<String> = emptyList(),
    val isActive: Boolean = true,
    val completionDates: Set<String> = emptySet(),
    val metadata: RecordMetadata,
)

data class NewSelfCareRoutine(
    val name: String,
    val description: String? = null,
    val category: SelfCareCategory = SelfCareCategory.Mind,
    val scheduledTimeMinutes: Int? = null,
    val repeatDays: Set<SelfCareDay> = emptySet(),
    val checklist: List<String> = emptyList(),
)

enum class SelfCareCategory { Mind, Body, Home, Joy }

enum class SelfCareDay { Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday }
