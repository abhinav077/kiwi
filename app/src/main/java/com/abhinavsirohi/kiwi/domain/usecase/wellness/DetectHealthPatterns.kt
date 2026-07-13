package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthPatternDetection
import com.abhinavsirohi.kiwi.domain.model.HealthPatternType
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DetectHealthPatterns {
    operator fun invoke(
        cycles: List<CycleRecord>,
        dailyRecords: List<WellnessDailyRecord>,
        today: LocalDate,
    ): List<HealthPatternDetection> = cycles
        .filter { it.endDate == null }
        .mapNotNull { cycle -> cycle.startDate.toDateOrNull()?.let { it to cycle } }
        .flatMap { (start, cycle) ->
            val records = dailyRecords
                .filter { it.cycleLocalId == cycle.localId }
                .mapNotNull { record -> record.recordDate.toDateOrNull()?.let { it to record } }
                .sortedBy { it.first }
            buildList {
                records.filter { it.second.painLevel?.let { pain -> pain >= HIGH_PAIN_LEVEL } == true }
                    .takeIf { it.size >= REPEATED_EVIDENCE_COUNT }
                    ?.let { add(detection(HealthPatternType.RepeatedHighPain, cycle.localId, it.map(Pair<LocalDate, WellnessDailyRecord>::first))) }
                records.filter { it.second.flow == WellnessFlow.Heavy }.map { it.first }
                    .consecutiveRun(REPEATED_EVIDENCE_COUNT)
                    ?.let { add(detection(HealthPatternType.ConsecutiveHeavyFlow, cycle.localId, it)) }
                val openDays = ChronoUnit.DAYS.between(start, today).toInt() + 1
                if (openDays >= PROLONGED_OPEN_DAYS) {
                    add(HealthPatternDetection(
                        HealthPatternType.ProlongedOpenPeriod,
                        cycle.localId,
                        openDays,
                        start.toString(),
                        today.toString(),
                    ))
                }
            }
        }

    private fun detection(type: HealthPatternType, cycleId: String, dates: List<LocalDate>) =
        HealthPatternDetection(type, cycleId, dates.size, dates.first().toString(), dates.last().toString())

    private fun List<LocalDate>.consecutiveRun(minimum: Int): List<LocalDate>? {
        if (size < minimum) return null
        var run = mutableListOf(first())
        for (date in drop(1)) {
            run = if (ChronoUnit.DAYS.between(run.last(), date) == 1L) {
                run.apply { add(date) }
            } else {
                mutableListOf(date)
            }
            if (run.size >= minimum) return run
        }
        return null
    }

    private fun String.toDateOrNull() = runCatching { LocalDate.parse(this) }.getOrNull()

    private companion object {
        const val HIGH_PAIN_LEVEL = 7
        const val REPEATED_EVIDENCE_COUNT = 2
        const val PROLONGED_OPEN_DAYS = 8
    }
}
