package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryPhoto
import kotlinx.coroutines.flow.Flow

interface DiaryPhotoRepository {
    fun observePhotos(): Flow<AppResult<List<DiaryPhoto>>>
    suspend fun addPhoto(diaryEntryLocalId: String, sourceUri: String): AppResult<DiaryPhoto>
    suspend fun deletePhoto(localId: String, deletedAt: Long): AppResult<Unit>
}
