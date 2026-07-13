package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "diary_photos",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["localId"],
            childColumns = ["diary_entry_local_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("diary_entry_local_id"),
        Index(value = ["user_id", "updated_at"]),
    ],
)
data class DiaryPhotoEntity(
    @PrimaryKey val localId: String,
    @ColumnInfo(name = "diary_entry_local_id") val diaryEntryLocalId: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "remote_path") val remotePath: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/jpeg",
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "byte_size") val byteSize: Long,
    @Embedded val syncMetadata: SyncMetadata,
)
