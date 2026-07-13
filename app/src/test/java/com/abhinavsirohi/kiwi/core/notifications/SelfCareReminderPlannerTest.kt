package com.abhinavsirohi.kiwi.core.notifications

import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelfCareReminderPlannerTest {
    private val planner = SelfCareReminderPlanner(ZoneId.of("UTC"), { 1_784_100_000_000L })

    @Test
    fun futureWeekdayRoutine_schedulesNextMatchingDay() {
        val planned = planner.plan(routine(setOf(SelfCareDay.Wednesday), 9 * 60 + 30))

        assertEquals("routine-1", planned?.routineLocalId)
        assertEquals(1_784_107_800_000L, planned?.triggerAtMillis)
    }

    @Test
    fun inactiveAndUntimedRoutines_doNotSchedule() {
        assertNull(planner.plan(routine(emptySet(), null)))
        assertNull(planner.plan(routine(emptySet(), 600).copy(isActive = false)))
    }

    private fun routine(days: Set<SelfCareDay>, minutes: Int?) = SelfCareRoutine(
        localId = "routine-1", name = "Pause", repeatDays = days, scheduledTimeMinutes = minutes,
        metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1"),
    )
}
