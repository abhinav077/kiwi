package com.abhinavsirohi.kiwi.data.local.entity

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abhinavsirohi.kiwi.core.database.SyncMetadata

@Entity(
    tableName = "wellness_daily_records",
    indices = [
        Index(value = ["user_id", "record_date"], unique = true),
        Index(value = ["cycle_local_id"]),
    ],
)
data class WellnessDailyRecordEntity(
    @PrimaryKey
    val localId: String,
    @ColumnInfo(name = "record_date")
    val recordDate: String,
    @ColumnInfo(name = "cycle_local_id")
    val cycleLocalId: String? = null,
    val flow: String? = null,
    @ColumnInfo(name = "pain_level")
    val painLevel: Int? = null,
    @ColumnInfo(name = "cramps_level")
    val crampsLevel: Int? = null,
    @ColumnInfo(name = "symptoms_json")
    val symptomsJson: String? = null,
    val mood: String? = null,
    @ColumnInfo(name = "energy_level")
    val energyLevel: Int? = null,
    @ColumnInfo(name = "sleep_minutes")
    val sleepMinutes: Int? = null,
    val notes: String? = null,
    val exercise: String? = null,
    @ColumnInfo(name = "self_care_medication_notes")
    val selfCareMedicationNotes: String? = null,
    @Embedded
    val syncMetadata: SyncMetadata,
)
