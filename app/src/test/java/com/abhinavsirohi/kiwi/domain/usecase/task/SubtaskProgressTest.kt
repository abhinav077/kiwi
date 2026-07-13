package com.abhinavsirohi.kiwi.domain.usecase.task

import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.calculateSubtaskProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtaskProgressTest {
    @Test
    fun emptySubtasks_haveZeroProgress() {
        assertEquals(0f, calculateSubtaskProgress(emptyList()), 0f)
    }

    @Test
    fun progress_countsCompletedSubtasks() {
        val metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1")
        val subtasks = listOf(
            Subtask("one", "task", "One", isCompleted = true, metadata = metadata),
            Subtask("two", "task", "Two", isCompleted = false, metadata = metadata),
            Subtask("three", "task", "Three", isCompleted = true, metadata = metadata),
        )

        assertEquals(2f / 3f, calculateSubtaskProgress(subtasks), 0.0001f)
    }
}
