package com.abhinavsirohi.kiwi.core.notifications

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthAlertNotificationPlannerTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun daytime_deliversImmediately() {
        val now = Instant.parse("2026-07-14T10:00:00Z").toEpochMilli()
        assertEquals(now, HealthAlertNotificationPlanner(utc) { now }.plan("episode").triggerAtMillis)
    }

    @Test
    fun quietHours_deferUntilEightInMorning() {
        val now = Instant.parse("2026-07-14T22:00:00Z").toEpochMilli()
        val expected = Instant.parse("2026-07-15T08:00:00Z").toEpochMilli()
        assertEquals(expected, HealthAlertNotificationPlanner(utc) { now }.plan("episode").triggerAtMillis)
    }
}
