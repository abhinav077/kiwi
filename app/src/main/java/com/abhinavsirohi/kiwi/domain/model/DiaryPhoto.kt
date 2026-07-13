package com.abhinavsirohi.kiwi.domain.model

data class DiaryPhoto(
    val localId: String,
    val diaryEntryLocalId: String,
    val localPath: String,
    val remotePath: String? = null,
    val mimeType: String = "image/jpeg",
    val width: Int,
    val height: Int,
    val byteSize: Long,
    val metadata: RecordMetadata,
)
