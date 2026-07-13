package com.abhinavsirohi.kiwi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WellnessRecordsTest {
    @Test
    fun cycleDates_allowOpenAndCompletedPeriods() {
        assertEquals("2026-07-14" to null, validateCycleDates("2026-07-14", null))
        assertEquals("2026-07-14" to "2026-07-18", validateCycleDates("2026-07-14", "2026-07-18"))
    }

    @Test
    fun cycleDates_rejectInvalidOrder() {
        assertThrows(InvalidWellnessRecordException::class.java) {
            validateCycleDates("2026-07-14", "2026-07-13")
        }
    }

    @Test
    fun dailyRecordDate_requiresIsoLocalDate() {
        assertEquals("2026-07-14", validateWellnessRecordDate("2026-07-14"))
        assertThrows(InvalidWellnessRecordException::class.java) {
            validateWellnessRecordDate("14/07/2026")
        }
    }

    @Test
    fun dailyFields_normalizeTextAndSymptoms() {
        val fields = validateWellnessDailyFields(
            flow = WellnessFlow.Medium,
            painLevel = 4,
            crampsLevel = 6,
            symptoms = listOf(" Fatigue ", "Fatigue", "", "Headache"),
            mood = "  Steady ",
            energyLevel = 5,
            sleepMinutes = 450,
            notes = "  Rested after lunch ",
            exercise = " Walk ",
            selfCareMedicationNotes = " Tea ",
        )

        assertEquals(WellnessFlow.Medium, fields.flow)
        assertEquals(listOf("Fatigue", "Headache"), fields.symptoms)
        assertEquals("Steady", fields.mood)
        assertEquals("Rested after lunch", fields.notes)
    }

    @Test
    fun dailyFields_rejectOutOfRangeScoresAndSleep() {
        assertThrows(InvalidWellnessRecordException::class.java) {
            validateWellnessDailyFields(
                flow = null,
                painLevel = 11,
                crampsLevel = null,
                symptoms = emptyList(),
                mood = null,
                energyLevel = null,
                sleepMinutes = null,
                notes = null,
                exercise = null,
                selfCareMedicationNotes = null,
            )
        }
        assertThrows(InvalidWellnessRecordException::class.java) {
            validateWellnessDailyFields(
                flow = null,
                painLevel = null,
                crampsLevel = null,
                symptoms = emptyList(),
                mood = null,
                energyLevel = null,
                sleepMinutes = 1441,
                notes = null,
                exercise = null,
                selfCareMedicationNotes = null,
            )
        }
    }
}
