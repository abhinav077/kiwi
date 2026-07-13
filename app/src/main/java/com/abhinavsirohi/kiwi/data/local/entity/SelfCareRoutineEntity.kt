package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(tableName = "self_care_routines")
data class SelfCareRoutineEntity(
    @PrimaryKey val localId: String,
    val name: String,
    val description: String?,
    val category: String,
    val scheduledTimeMinutes: Int?,
    val repeatDays: String,
    val checklist: String,
    val isActive: Boolean,
    val completionDates: String,
    @Embedded val syncMetadata: SyncMetadata,
)
