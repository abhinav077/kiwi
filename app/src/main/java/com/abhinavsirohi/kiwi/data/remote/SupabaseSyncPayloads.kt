package com.abhinavsirohi.kiwi.data.remote

import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.CycleRecordEntity
import com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity
import com.abhinavsirohi.kiwi.data.local.entity.HealthAlertEpisodeEntity
import com.abhinavsirohi.kiwi.data.local.entity.ProfileEntity
import com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskPostponementEntity
import com.abhinavsirohi.kiwi.data.local.entity.WeeklyReflectionEntity
import com.abhinavsirohi.kiwi.data.local.entity.WellnessDailyRecordEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ApplyKiwiChangeParameters(
    @SerialName("record_type") val recordType: String,
    val payload: JsonObject,
)

internal fun syncPayload(recordType: SyncRecordType, record: Any, profileEmail: String?): JsonObject =
    buildJsonObject {
        when (recordType) {
            SyncRecordType.TASK -> (record as TaskEntity).let { value ->
                putCommon(value.localId, value.syncMetadata)
                put("title", value.title); putNullable("description", value.description)
                put("category", value.category); put("priority", value.priority); putNullable("notes", value.notes)
                putNullable("scheduled_date", value.scheduledDate); putNullable("scheduled_time_minutes", value.scheduledTimeMinutes)
                put("recurrence_frequency", value.recurrenceFrequency); put("recurrence_interval", value.recurrenceInterval)
                putNullable("recurrence_end_date", value.recurrenceEndDate); putNullable("recurrence_series_id", value.recurrenceSeriesId)
                put("is_completed", value.isCompleted); put("position", value.position)
            }
            SyncRecordType.SUBTASK -> (record as SubtaskEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("task_local_id", value.taskLocalId)
                put("title", value.title); put("is_completed", value.isCompleted); put("position", value.position)
            }
            SyncRecordType.PROFILE -> (record as ProfileEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("email", profileEmail.orEmpty())
                put("preferred_name", value.preferredName)
            }
            SyncRecordType.CYCLE_RECORD -> (record as CycleRecordEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("start_date", value.startDate); putNullable("end_date", value.endDate)
            }
            SyncRecordType.WELLNESS_DAILY_RECORD -> (record as WellnessDailyRecordEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("record_date", value.recordDate)
                putNullable("cycle_local_id", value.cycleLocalId); putNullable("flow", value.flow)
                putNullable("pain_level", value.painLevel); putNullable("cramps_level", value.crampsLevel)
                putNullable("symptoms_json", value.symptomsJson); putNullable("mood", value.mood)
                putNullable("energy_level", value.energyLevel); putNullable("sleep_minutes", value.sleepMinutes)
                putNullable("notes", value.notes); putNullable("exercise", value.exercise)
                putNullable("self_care_medication_notes", value.selfCareMedicationNotes)
            }
            SyncRecordType.HEALTH_ALERT_EPISODE -> (record as HealthAlertEpisodeEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("pattern_type", value.patternType)
                put("source_cycle_id", value.sourceCycleId); put("evidence_count", value.evidenceCount)
                put("first_evidence_date", value.firstEvidenceDate); put("last_evidence_date", value.lastEvidenceDate)
                put("state", value.state); putNullable("notified_at", value.notifiedAt)
            }
            SyncRecordType.DIARY_ENTRY -> (record as DiaryEntryEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("title", value.title); put("content", value.content)
                put("entry_date", value.entryDate); putNullable("best_thing", value.bestThing)
                putNullable("mood", value.mood); put("is_favourite", value.isFavourite)
            }
            SyncRecordType.SELF_CARE_ROUTINE -> (record as SelfCareRoutineEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("name", value.name)
                putNullable("description", value.description); put("category", value.category)
                putNullable("scheduled_time_minutes", value.scheduledTimeMinutes); put("repeat_days", value.repeatDays)
                put("checklist", value.checklist); put("is_active", value.isActive); put("completion_dates", value.completionDates)
            }
            SyncRecordType.TASK_POSTPONEMENT -> (record as TaskPostponementEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("task_local_id", value.taskLocalId)
                put("task_title", value.taskTitle); put("previous_date", value.previousDate); put("new_date", value.newDate)
                put("postponed_at", value.postponedAt)
            }
            SyncRecordType.WEEKLY_REFLECTION -> (record as WeeklyReflectionEntity).let { value ->
                putCommon(value.localId, value.syncMetadata); put("week_start", value.weekStart); put("content", value.content)
            }
            SyncRecordType.DIARY_PHOTO -> error("Diary photos use the Storage sync worker")
        }
    }

private fun kotlinx.serialization.json.JsonObjectBuilder.putCommon(localId: String, metadata: SyncMetadata) {
    put("local_id", localId); put("user_id", metadata.userId); put("created_at", metadata.createdAt)
    put("updated_at", metadata.updatedAt); putNullable("deleted_at", metadata.deletedAt); put("device_id", metadata.deviceId)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Any?) {
    put(key, when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        else -> error("Unsupported sync value for $key")
    })
}
