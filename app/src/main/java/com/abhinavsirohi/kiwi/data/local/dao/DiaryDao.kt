package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity
import com.abhinavsirohi.kiwi.data.local.entity.DiaryPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Upsert suspend fun upsert(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE localId = :localId")
    suspend fun find(localId: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE user_id = :userId AND deleted_at IS NULL ORDER BY entry_date DESC, updated_at DESC")
    fun observeActive(userId: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getAwaitingSync(): List<DiaryEntryEntity>

    @Upsert suspend fun upsertPhoto(photo: DiaryPhotoEntity)

    @Query("SELECT * FROM diary_photos WHERE localId = :localId")
    suspend fun findPhoto(localId: String): DiaryPhotoEntity?

    @Query("SELECT * FROM diary_photos WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at")
    fun observeActivePhotos(userId: String): Flow<List<DiaryPhotoEntity>>

    @Query("SELECT * FROM diary_photos WHERE diary_entry_local_id = :entryLocalId AND user_id = :userId AND deleted_at IS NULL")
    suspend fun getActivePhotosForEntry(entryLocalId: String, userId: String): List<DiaryPhotoEntity>

    @Query("SELECT * FROM diary_photos WHERE user_id = :userId AND sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getPhotosAwaitingSync(userId: String): List<DiaryPhotoEntity>
}
