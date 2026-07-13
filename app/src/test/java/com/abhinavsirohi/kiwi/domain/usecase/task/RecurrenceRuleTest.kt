package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.domain.model.InvalidRecurrenceRuleException
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.RecurrenceRule
import com.abhinavsirohi.kiwi.domain.model.nextOccurrenceDate
import com.abhinavsirohi.kiwi.domain.model.validateRecurrenceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecurrenceRuleTest {
    @Test
    fun nextOccurrence_supportsExplicitIntervals() {
        assertEquals("2026-07-17", nextOccurrenceDate("2026-07-14", RecurrenceRule(RecurrenceFrequency.Daily, 3)))
        assertEquals("2026-07-28", nextOccurrenceDate("2026-07-14", RecurrenceRule(RecurrenceFrequency.Weekly, 2)))
        assertEquals("2026-09-14", nextOccurrenceDate("2026-07-14", RecurrenceRule(RecurrenceFrequency.Monthly, 2)))
    }

    @Test
    fun monthlyRecurrence_clampsToLastValidDay() {
        assertEquals("2026-02-28", nextOccurrenceDate("2026-01-31", RecurrenceRule(RecurrenceFrequency.Monthly)))
    }

    @Test
    fun endDate_preventsOccurrencesAfterBoundary() {
        val rule = RecurrenceRule(RecurrenceFrequency.Daily, endDate = "2026-07-14")
        assertEquals(null, nextOccurrenceDate("2026-07-14", rule))
    }

    @Test
    fun recurringTask_requiresDateAndPositiveInterval() {
        assertThrows(InvalidRecurrenceRuleException::class.java) {
            validateRecurrenceRule(null, RecurrenceRule(RecurrenceFrequency.Weekly))
        }
        assertThrows(InvalidRecurrenceRuleException::class.java) {
            validateRecurrenceRule("2026-07-14", RecurrenceRule(RecurrenceFrequency.Daily, interval = 0))
        }
    }
}
