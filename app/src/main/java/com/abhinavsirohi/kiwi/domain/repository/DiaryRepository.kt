package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.NewDiaryEntry
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun observeEntries(): Flow<AppResult<List<DiaryEntry>>>
    suspend fun createEntry(entry: NewDiaryEntry): AppResult<DiaryEntry>
    suspend fun saveEntry(entry: DiaryEntry): AppResult<Unit>
    suspend fun tombstoneEntry(localId: String, deletedAt: Long): AppResult<Unit>
}
