package com.abhinavsirohi.kiwi.core.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRunnerTest {
    @Test
    fun processorResults_mapToWorkerOutcomesWithoutDiscardingErrors() = runBlocking {
        val processor = FakeSyncProcessor()
        val runner = SyncRunner(processor)

        processor.result = SyncProcessingResult.QueueDrained
        assertEquals(SyncWorkerOutcome.SUCCESS, runner.run())

        processor.result = SyncProcessingResult.Retry(IllegalStateException("offline"))
        assertEquals(SyncWorkerOutcome.RETRY, runner.run())

        processor.result = SyncProcessingResult.PermanentFailure(IllegalArgumentException("rejected"))
        assertEquals(SyncWorkerOutcome.FAILURE, runner.run())
    }
}

private class FakeSyncProcessor : SyncProcessor {
    var result: SyncProcessingResult = SyncProcessingResult.QueueDrained

    override suspend fun processPendingChanges(): SyncProcessingResult = result
}
