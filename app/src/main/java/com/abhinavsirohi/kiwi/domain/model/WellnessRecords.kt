package com.abhinavsirohi.kiwi.domain.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

data class NewCycleRecord(
    val startDate: String,
    val endDate: String? = null,
)

data class CycleRecord(
    val localId: String,
    val startDate: String,
    val endDate: String? = null,
    val metadata: RecordMetadata,
)

data class NewWellnessDailyRecord(
    val recordDate: String,
    val cycleLocalId: String? = null,
    val flow: WellnessFlow? = null,
    val painLevel: Int? = null,
    val crampsLevel: Int? = null,
    val symptoms: List<String> = emptyList(),
    val mood: String? = null,
    val energyLevel: Int? = null,
    val sleepMinutes: Int? = null,
    val notes: String? = null,
    val exercise: String? = null,
    val selfCareMedicationNotes: String? = null,
)

data class WellnessDailyRecord(
    val localId: String,
    val recordDate: String,
    val cycleLocalId: String? = null,
    val flow: WellnessFlow? = null,
    val painLevel: Int? = null,
    val crampsLevel: Int? = null,
    val symptoms: List<String> = emptyList(),
    val mood: String? = null,
    val energyLevel: Int? = null,
    val sleepMinutes: Int? = null,
    val notes: String? = null,
    val exercise: String? = null,
    val selfCareMedicationNotes: String? = null,
    val metadata: RecordMetadata,
)

enum class WellnessFlow {
    None,
    Light,
    Medium,
    Heavy,
}

data class WellnessDailyFields(
    val flow: WellnessFlow?,
    val painLevel: Int?,
    val crampsLevel: Int?,
    val symptoms: List<String>,
    val mood: String?,
    val energyLevel: Int?,
    val sleepMinutes: Int?,
    val notes: String?,
    val exercise: String?,
    val selfCareMedicationNotes: String?,
)

class InvalidWellnessRecordException(message: String) : IllegalArgumentException(message)

fun validateCycleDates(startDate: String, endDate: String?): Pair<String, String?> {
    val start = startDate.wellnessDateOrNull()
        ?: throw InvalidWellnessRecordException("Period start date is invalid")
    val end = endDate?.takeIf(String::isNotBlank)?.wellnessDateOrNull()
        ?: if (endDate.isNullOrBlank()) null else throw InvalidWellnessRecordException("Period end date is invalid")
    if (end != null && end < start) {
        throw InvalidWellnessRecordException("Period end date cannot be before its start date")
    }
    return start.toString() to end?.toString()
}

fun validateWellnessRecordDate(recordDate: String): String =
    recordDate.wellnessDateOrNull()?.toString()
        ?: throw InvalidWellnessRecordException("Daily record date is invalid")

fun validateWellnessDailyFields(
    flow: WellnessFlow?,
    painLevel: Int?,
    crampsLevel: Int?,
    symptoms: List<String>,
    mood: String?,
    energyLevel: Int?,
    sleepMinutes: Int?,
    notes: String?,
    exercise: String?,
    selfCareMedicationNotes: String?,
): WellnessDailyFields {
    validateScore("Pain", painLevel)
    validateScore("Cramps", crampsLevel)
    validateScore("Energy", energyLevel)
    if (sleepMinutes != null && sleepMinutes !in 0..(24 * 60)) {
        throw InvalidWellnessRecordException("Sleep duration must be between 0 and 1440 minutes")
    }
    return WellnessDailyFields(
        flow = flow,
        painLevel = painLevel,
        crampsLevel = crampsLevel,
        symptoms = symptoms.map(String::trim).filter(String::isNotEmpty).distinct(),
        mood = mood.cleaned(),
        energyLevel = energyLevel,
        sleepMinutes = sleepMinutes,
        notes = notes.cleaned(),
        exercise = exercise.cleaned(),
        selfCareMedicationNotes = selfCareMedicationNotes.cleaned(),
    )
}

private fun validateScore(label: String, value: Int?) {
    if (value != null && value !in 0..10) {
        throw InvalidWellnessRecordException("$label must be between 0 and 10")
    }
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String.wellnessDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}
