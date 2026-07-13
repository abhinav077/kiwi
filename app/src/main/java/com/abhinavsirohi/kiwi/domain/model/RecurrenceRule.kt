package com.abhinavsirohi.kiwi.domain.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

enum class RecurrenceFrequency {
    None,
    Daily,
    Weekly,
    Monthly,
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.None,
    val interval: Int = 1,
    val endDate: String? = null,
)

class InvalidRecurrenceRuleException(message: String) : IllegalArgumentException(message)

fun validateRecurrenceRule(scheduledDate: String?, rule: RecurrenceRule): RecurrenceRule {
    if (rule.frequency == RecurrenceFrequency.None) return RecurrenceRule()
    if (rule.interval < 1) throw InvalidRecurrenceRuleException("Repeat interval must be at least one")
    val start = scheduledDate.toLocalDateOrNull()
        ?: throw InvalidRecurrenceRuleException("Recurring tasks require a valid scheduled date")
    val end = rule.endDate?.takeIf(String::isNotBlank)?.toLocalDateOrNull()
        ?: if (rule.endDate.isNullOrBlank()) null else throw InvalidRecurrenceRuleException("Repeat end date is invalid")
    if (end != null && end < start) throw InvalidRecurrenceRuleException("Repeat end date is before the task date")
    return rule.copy(endDate = end?.toString())
}

fun nextOccurrenceDate(currentDate: String, rule: RecurrenceRule): String? {
    if (rule.frequency == RecurrenceFrequency.None || rule.interval < 1) return null
    val current = currentDate.toLocalDateOrNull() ?: return null
    val next = when (rule.frequency) {
        RecurrenceFrequency.None -> return null
        RecurrenceFrequency.Daily -> current.plusDays(rule.interval.toLong())
        RecurrenceFrequency.Weekly -> current.plusWeeks(rule.interval.toLong())
        RecurrenceFrequency.Monthly -> current.plusMonths(rule.interval.toLong())
    }
    val end = rule.endDate?.toLocalDateOrNull()
    return next.takeIf { end == null || it <= end }?.toString()
}

private fun String?.toLocalDateOrNull(): LocalDate? = try {
    this?.let(LocalDate::parse)
} catch (_: DateTimeParseException) {
    null
}
