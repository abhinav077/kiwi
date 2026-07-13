package com.abhinavsirohi.kiwi.domain.model

enum class HealthPatternType { RepeatedHighPain, ConsecutiveHeavyFlow, ProlongedOpenPeriod }
enum class HealthAlertState { Active, Acknowledged, Dismissed, Resolved }

data class HealthPatternDetection(
    val patternType: HealthPatternType,
    val sourceCycleId: String,
    val evidenceCount: Int,
    val firstEvidenceDate: String,
    val lastEvidenceDate: String,
)

data class HealthAlertEpisode(
    val localId: String,
    val patternType: HealthPatternType,
    val sourceCycleId: String,
    val evidenceCount: Int,
    val firstEvidenceDate: String,
    val lastEvidenceDate: String,
    val state: HealthAlertState,
    val notifiedAt: Long? = null,
    val metadata: RecordMetadata,
)

fun HealthPatternType.cautionTitle(): String = when (this) {
    HealthPatternType.RepeatedHighPain -> "Repeated high pain was recorded"
    HealthPatternType.ConsecutiveHeavyFlow -> "Heavy flow was recorded on consecutive days"
    HealthPatternType.ProlongedOpenPeriod -> "This period record has remained open"
}

fun HealthPatternType.cautionBody(): String =
    "This is a summary of your recorded entries, not a diagnosis. If it concerns you, consider discussing it with a qualified health professional."
