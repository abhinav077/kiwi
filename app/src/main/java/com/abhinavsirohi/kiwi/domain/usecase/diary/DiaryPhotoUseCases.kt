package com.abhinavsirohi.kiwi.domain.usecase.diary

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryPhoto
import com.abhinavsirohi.kiwi.domain.repository.DiaryPhotoRepository
import kotlinx.coroutines.flow.Flow

class ObserveDiaryPhotos(private val repository: DiaryPhotoRepository) {
    operator fun invoke(): Flow<AppResult<List<DiaryPhoto>>> = repository.observePhotos()
}

data class AddDiaryPhotoParameters(val diaryEntryLocalId: String, val sourceUri: String)

class AddDiaryPhoto(private val repository: DiaryPhotoRepository) {
    suspend operator fun invoke(parameters: AddDiaryPhotoParameters): AppResult<DiaryPhoto> =
        repository.addPhoto(parameters.diaryEntryLocalId, parameters.sourceUri)
}

data class DeleteDiaryPhotoParameters(val localId: String, val deletedAt: Long)

class DeleteDiaryPhoto(private val repository: DiaryPhotoRepository) {
    suspend operator fun invoke(parameters: DeleteDiaryPhotoParameters): AppResult<Unit> =
        repository.deletePhoto(parameters.localId, parameters.deletedAt)
}
