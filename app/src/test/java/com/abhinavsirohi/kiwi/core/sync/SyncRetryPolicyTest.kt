package com.abhinavsirohi.kiwi.core.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRetryPolicyTest {
    private val policy = SyncRetryPolicy(initialDelayMillis = 1_000L, maximumDelayMillis = 8_000L)

    @Test
    fun retryDelay_growsExponentiallyAndIsCapped() {
        assertEquals(2_000L, policy.nextAttemptAt(failedAt = 1_000L, attemptCount = 1))
        assertEquals(3_000L, policy.nextAttemptAt(failedAt = 1_000L, attemptCount = 2))
        assertEquals(9_000L, policy.nextAttemptAt(failedAt = 1_000L, attemptCount = 10))
    }

    @Test
    fun lastWriteWins_acceptsOnlyStrictlyNewerUpdates() {
        assertTrue(LastWriteWins.shouldApplyIncoming(localUpdatedAt = 10L, incomingUpdatedAt = 11L))
        assertFalse(LastWriteWins.shouldApplyIncoming(localUpdatedAt = 10L, incomingUpdatedAt = 10L))
        assertFalse(LastWriteWins.shouldApplyIncoming(localUpdatedAt = 10L, incomingUpdatedAt = 9L))
    }
}
