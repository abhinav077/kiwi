package com.abhinavsirohi.kiwi.domain.usecase.diary

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.NewDiaryEntry
import com.abhinavsirohi.kiwi.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow

class ObserveDiaryEntries(private val repository: DiaryRepository) {
    operator fun invoke(): Flow<AppResult<List<DiaryEntry>>> = repository.observeEntries()
}

class CreateDiaryEntry(private val repository: DiaryRepository) {
    suspend operator fun invoke(entry: NewDiaryEntry): AppResult<DiaryEntry> {
        if (entry.title.trim().isEmpty()) return AppResult.Failure(IllegalArgumentException("Diary title is required"))
        if (entry.content.trim().isEmpty()) return AppResult.Failure(IllegalArgumentException("Diary content is required"))
        return repository.createEntry(entry.copy(title = entry.title.trim(), content = entry.content.trim()))
    }
}

class SaveDiaryEntry(private val repository: DiaryRepository) {
    suspend operator fun invoke(entry: DiaryEntry): AppResult<Unit> = repository.saveEntry(entry)
}

data class TombstoneDiaryEntryParameters(val localId: String, val deletedAt: Long)

class TombstoneDiaryEntry(private val repository: DiaryRepository) {
    suspend operator fun invoke(parameters: TombstoneDiaryEntryParameters): AppResult<Unit> =
        repository.tombstoneEntry(parameters.localId, parameters.deletedAt)
}
