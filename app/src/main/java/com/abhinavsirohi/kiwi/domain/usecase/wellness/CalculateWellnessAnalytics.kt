package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.DatedWellnessLabel
import com.abhinavsirohi.kiwi.domain.model.DatedWellnessLabels
import com.abhinavsirohi.kiwi.domain.model.DatedWellnessScore
import com.abhinavsirohi.kiwi.domain.model.WellnessAnalytics
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class CalculateWellnessAnalytics {
    operator fun invoke(
        cycleRecords: List<CycleRecord>,
        dailyRecords: List<WellnessDailyRecord>,
    ): WellnessAnalytics {
        val cycleStarts = cycleRecords
            .mapNotNull { it.startDate.toLocalDateOrNull() }
            .distinct()
            .sorted()
        val cycleLengths = cycleStarts.zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second).toInt()
        }.filter { it > 0 }
        val bleedingDurations = cycleRecords.mapNotNull { record ->
            val start = record.startDate.toLocalDateOrNull() ?: return@mapNotNull null
            val end = record.endDate?.toLocalDateOrNull() ?: return@mapNotNull null
            ChronoUnit.DAYS.between(start, end).toInt().takeIf { it >= 0 }?.plus(1)
        }.sorted()
        val datedRecords = dailyRecords.mapNotNull { record ->
            record.recordDate.toLocalDateOrNull()?.let { date -> date to record }
        }.sortedBy { it.first }
        val shortest = cycleLengths.minOrNull()
        val longest = cycleLengths.maxOrNull()

        return WellnessAnalytics(
            recordedCycleCount = cycleRecords.size,
            cycleLengthsDays = cycleLengths,
            averageCycleLengthDays = cycleLengths.averageOrNull(),
            shortestCycleDays = shortest,
            longestCycleDays = longest,
            cycleVariationDays = if (shortest != null && longest != null) longest - shortest else null,
            bleedingDurationsDays = bleedingDurations,
            averageBleedingDurationDays = bleedingDurations.averageOrNull(),
            flowDistribution = WellnessFlow.entries.mapNotNull { flow ->
                datedRecords.count { it.second.flow == flow }.takeIf { it > 0 }?.let { flow to it }
            }.toMap(),
            painHistory = datedRecords.mapNotNull { (_, record) ->
                record.painLevel?.let { DatedWellnessScore(record.recordDate, it) }
            },
            symptomHistory = datedRecords.mapNotNull { (_, record) ->
                record.symptoms.takeIf { it.isNotEmpty() }?.let { DatedWellnessLabels(record.recordDate, it) }
            },
            symptomOccurrences = datedRecords.flatMap { it.second.symptoms }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .groupingBy { it.lowercase() }
                .eachCount()
                .toSortedMap(),
            moodHistory = datedRecords.mapNotNull { (_, record) ->
                record.mood?.takeIf(String::isNotBlank)?.let { DatedWellnessLabel(record.recordDate, it) }
            },
            moodOccurrences = datedRecords.mapNotNull { it.second.mood?.trim()?.takeIf(String::isNotEmpty) }
                .groupingBy { it.lowercase() }
                .eachCount()
                .toSortedMap(),
            energyHistory = datedRecords.mapNotNull { (_, record) ->
                record.energyLevel?.let { DatedWellnessScore(record.recordDate, it) }
            },
        )
    }

    private fun List<Int>.averageOrNull(): Double? = takeIf { it.isNotEmpty() }?.average()

    private fun String.toLocalDateOrNull(): LocalDate? = try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}
