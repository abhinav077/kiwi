package com.abhinavsirohi.kiwi.domain.usecase.diary

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.NewDiaryEntry
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryUseCasesTest {
    @Test
    fun createTrimsRequiredAndOptionalText() = runTest {
        val repository = FakeDiaryRepository()
        val result = CreateDiaryEntry(repository)(NewDiaryEntry("  Evening  ", "  A good day  ", "2026-07-14", "  Walk  ", "  Calm  "))

        assertTrue(result is AppResult.Success)
        assertEquals("Evening", repository.created?.title)
        assertEquals("A good day", repository.created?.content)
    }

    @Test
    fun createRejectsBlankWriting() = runTest {
        val result = CreateDiaryEntry(FakeDiaryRepository())(NewDiaryEntry("Title", "   ", "2026-07-14"))

        assertTrue(result is AppResult.Failure)
    }

    private class FakeDiaryRepository : DiaryRepository {
        var created: NewDiaryEntry? = null
        override fun observeEntries(): Flow<AppResult<List<DiaryEntry>>> = emptyFlow()
        override suspend fun createEntry(entry: NewDiaryEntry): AppResult<DiaryEntry> {
            created = entry
            return AppResult.Success(
                DiaryEntry(
                    "id", entry.title, entry.content, entry.entryDate,
                    metadata = RecordMetadata(userId = "user", createdAt = 0, updatedAt = 0, deviceId = "device"),
                ),
            )
        }
        override suspend fun saveEntry(entry: DiaryEntry) = AppResult.Success(Unit)
        override suspend fun tombstoneEntry(localId: String, deletedAt: Long) = AppResult.Success(Unit)
    }
}
