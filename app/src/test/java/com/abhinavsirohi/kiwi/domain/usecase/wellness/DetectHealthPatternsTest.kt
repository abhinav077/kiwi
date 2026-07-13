package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthPatternType
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectHealthPatternsTest {
    private val detect = DetectHealthPatterns()

    @Test
    fun activeCycle_detectsOnlyApprovedRepeatedRecordedPatterns() {
        val cycle = cycle("cycle-1", "2026-07-01")
        val records = listOf(
            daily("d1", "2026-07-01", cycle.localId, 7, WellnessFlow.Heavy),
            daily("d2", "2026-07-02", cycle.localId, 8, WellnessFlow.Heavy),
        )

        val result = detect(listOf(cycle), records, LocalDate.parse("2026-07-08"))

        assertEquals(
            setOf(HealthPatternType.RepeatedHighPain, HealthPatternType.ConsecutiveHeavyFlow, HealthPatternType.ProlongedOpenPeriod),
            result.map { it.patternType }.toSet(),
        )
    }

    @Test
    fun singleOrNonConsecutiveEvidence_doesNotCreateRepeatedPattern() {
        val cycle = cycle("cycle-1", "2026-07-01")
        val records = listOf(
            daily("d1", "2026-07-01", cycle.localId, 7, WellnessFlow.Heavy),
            daily("d2", "2026-07-03", cycle.localId, 2, WellnessFlow.Heavy),
        )

        val result = detect(listOf(cycle), records, LocalDate.parse("2026-07-03"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun completedCycle_isNotTreatedAsAnActiveSituation() {
        val cycle = cycle("cycle-1", "2026-07-01").copy(endDate = "2026-07-08")
        val records = listOf(
            daily("d1", "2026-07-01", cycle.localId, 9, WellnessFlow.Heavy),
            daily("d2", "2026-07-02", cycle.localId, 9, WellnessFlow.Heavy),
        )
        assertTrue(detect(listOf(cycle), records, LocalDate.parse("2026-07-08")).isEmpty())
    }

    private fun cycle(id: String, start: String) = CycleRecord(id, start, metadata = metadata(id))
    private fun daily(id: String, date: String, cycleId: String, pain: Int, flow: WellnessFlow) = WellnessDailyRecord(
        localId = id, recordDate = date, cycleLocalId = cycleId, flow = flow, painLevel = pain, metadata = metadata(id),
    )
    private fun metadata(id: String) = RecordMetadata(userId = "user-1", createdAt = 1, updatedAt = 1, deviceId = id)
}
