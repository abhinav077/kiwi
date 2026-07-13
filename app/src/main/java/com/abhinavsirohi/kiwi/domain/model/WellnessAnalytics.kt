package com.abhinavsirohi.kiwi.domain.model

data class DatedWellnessScore(
    val date: String,
    val value: Int,
)

data class DatedWellnessLabels(
    val date: String,
    val values: List<String>,
)

data class DatedWellnessLabel(
    val date: String,
    val value: String,
)

data class WellnessAnalytics(
    val recordedCycleCount: Int = 0,
    val cycleLengthsDays: List<Int> = emptyList(),
    val averageCycleLengthDays: Double? = null,
    val shortestCycleDays: Int? = null,
    val longestCycleDays: Int? = null,
    val cycleVariationDays: Int? = null,
    val bleedingDurationsDays: List<Int> = emptyList(),
    val averageBleedingDurationDays: Double? = null,
    val flowDistribution: Map<WellnessFlow, Int> = emptyMap(),
    val painHistory: List<DatedWellnessScore> = emptyList(),
    val symptomHistory: List<DatedWellnessLabels> = emptyList(),
    val symptomOccurrences: Map<String, Int> = emptyMap(),
    val moodHistory: List<DatedWellnessLabel> = emptyList(),
    val moodOccurrences: Map<String, Int> = emptyMap(),
    val energyHistory: List<DatedWellnessScore> = emptyList(),
)
