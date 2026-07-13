package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["user_id"], unique = true)],
)
data class ProfileEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "preferred_name")
    val preferredName: String,
    @Embedded
    val syncMetadata: SyncMetadata,
)
