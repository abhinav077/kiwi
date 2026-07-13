package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculateWellnessAnalyticsTest {
    private val calculate = CalculateWellnessAnalytics()

    @Test
    fun multipleRecords_calculateHistoricalFactsInDateOrder() {
        val cycles = listOf(
            cycle("cycle-3", "2026-02-28", "2026-03-04"),
            cycle("cycle-1", "2026-01-01", "2026-01-05"),
            cycle("cycle-2", "2026-01-29", "2026-02-02"),
        )
        val daily = listOf(
            daily("daily-2", "2026-01-02", WellnessFlow.Heavy, pain = 7, energy = 3, symptoms = listOf("Fatigue"), mood = "Low"),
            daily("daily-1", "2026-01-01", WellnessFlow.Light, pain = 3, energy = 6, symptoms = listOf("Fatigue", "Headache"), mood = "Calm"),
            daily("daily-3", "2026-01-03", WellnessFlow.Heavy, pain = null, energy = 5, symptoms = emptyList(), mood = "Calm"),
        )

        val result = calculate(cycles, daily)

        assertEquals(3, result.recordedCycleCount)
        assertEquals(listOf(28, 30), result.cycleLengthsDays)
        assertEquals(29.0, result.averageCycleLengthDays!!, 0.0)
        assertEquals(28, result.shortestCycleDays)
        assertEquals(30, result.longestCycleDays)
        assertEquals(2, result.cycleVariationDays)
        assertEquals(listOf(5, 5, 5), result.bleedingDurationsDays)
        assertEquals(5.0, result.averageBleedingDurationDays!!, 0.0)
        assertEquals(mapOf(WellnessFlow.Light to 1, WellnessFlow.Heavy to 2), result.flowDistribution)
        assertEquals(listOf("2026-01-01", "2026-01-02"), result.painHistory.map { it.date })
        assertEquals(mapOf("fatigue" to 2, "headache" to 1), result.symptomOccurrences)
        assertEquals(mapOf("calm" to 2, "low" to 1), result.moodOccurrences)
        assertEquals(listOf(6, 3, 5), result.energyHistory.map { it.value })
    }

    @Test
    fun sparseAndOpenRecords_reportUnavailableValuesWithoutGuessing() {
        val result = calculate(
            cycleRecords = listOf(cycle("cycle-1", "2026-01-01", null)),
            dailyRecords = emptyList(),
        )

        assertEquals(1, result.recordedCycleCount)
        assertEquals(emptyList<Int>(), result.cycleLengthsDays)
        assertNull(result.averageCycleLengthDays)
        assertNull(result.shortestCycleDays)
        assertNull(result.longestCycleDays)
        assertNull(result.cycleVariationDays)
        assertEquals(emptyList<Int>(), result.bleedingDurationsDays)
        assertNull(result.averageBleedingDurationDays)
    }

    @Test
    fun malformedAndDuplicateDates_areIgnoredForIntervalsAndHistory() {
        val result = calculate(
            cycleRecords = listOf(
                cycle("cycle-1", "not-a-date", null),
                cycle("cycle-2", "2026-01-01", null),
                cycle("cycle-3", "2026-01-01", null),
            ),
            dailyRecords = listOf(daily("daily-1", "bad-date", WellnessFlow.Medium, pain = 4)),
        )

        assertEquals(3, result.recordedCycleCount)
        assertEquals(emptyList<Int>(), result.cycleLengthsDays)
        assertEquals(emptyMap<WellnessFlow, Int>(), result.flowDistribution)
        assertEquals(emptyList<Int>(), result.painHistory.map { it.value })
    }

    private fun cycle(id: String, start: String, end: String?) = CycleRecord(
        localId = id,
        startDate = start,
        endDate = end,
        metadata = metadata(id),
    )

    private fun daily(
        id: String,
        date: String,
        flow: WellnessFlow,
        pain: Int? = null,
        energy: Int? = null,
        symptoms: List<String> = emptyList(),
        mood: String? = null,
    ) = WellnessDailyRecord(
        localId = id,
        recordDate = date,
        flow = flow,
        painLevel = pain,
        symptoms = symptoms,
        mood = mood,
        energyLevel = energy,
        metadata = metadata(id),
    )

    private fun metadata(id: String) = RecordMetadata(
        userId = "user-1",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deviceId = "device-1-$id",
    )
}
