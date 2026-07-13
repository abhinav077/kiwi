package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "health_alert_episodes",
    indices = [Index(value = ["user_id", "pattern_type", "source_cycle_id"], unique = true)],
)
data class HealthAlertEpisodeEntity(
    @PrimaryKey val localId: String,
    @ColumnInfo(name = "pattern_type") val patternType: String,
    @ColumnInfo(name = "source_cycle_id") val sourceCycleId: String,
    @ColumnInfo(name = "evidence_count") val evidenceCount: Int,
    @ColumnInfo(name = "first_evidence_date") val firstEvidenceDate: String,
    @ColumnInfo(name = "last_evidence_date") val lastEvidenceDate: String,
    val state: String,
    @ColumnInfo(name = "notified_at") val notifiedAt: Long? = null,
    @Embedded val syncMetadata: SyncMetadata,
)
