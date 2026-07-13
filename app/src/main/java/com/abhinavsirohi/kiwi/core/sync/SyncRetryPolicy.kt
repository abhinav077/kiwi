package com.abhinavsirohi.kiwi.core.sync

import kotlin.math.min

class SyncRetryPolicy(
    private val initialDelayMillis: Long = 30_000L,
    private val maximumDelayMillis: Long = 6 * 60 * 60 * 1_000L,
) {
    init {
        require(initialDelayMillis > 0)
        require(maximumDelayMillis >= initialDelayMillis)
    }

    fun nextAttemptAt(failedAt: Long, attemptCount: Int): Long {
        val exponent = (attemptCount - 1).coerceIn(0, MAX_EXPONENT)
        val multiplier = 1L shl exponent
        val delay = min(maximumDelayMillis, initialDelayMillis.saturatedMultiply(multiplier))
        return failedAt.saturatedAdd(delay)
    }

    private fun Long.saturatedMultiply(other: Long): Long =
        if (this > Long.MAX_VALUE / other) Long.MAX_VALUE else this * other

    private fun Long.saturatedAdd(other: Long): Long =
        if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

    private companion object {
        const val MAX_EXPONENT = 30
    }
}

object LastWriteWins {
    fun shouldApplyIncoming(localUpdatedAt: Long, incomingUpdatedAt: Long): Boolean =
        incomingUpdatedAt > localUpdatedAt
}
