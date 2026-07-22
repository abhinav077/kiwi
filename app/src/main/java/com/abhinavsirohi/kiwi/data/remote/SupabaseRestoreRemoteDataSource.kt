package com.abhinavsirohi.kiwi.data.remote

import com.abhinavsirohi.kiwi.core.sync.restore.RestoreRemoteDataSource
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreSnapshot
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.HealthAlertState
import com.abhinavsirohi.kiwi.domain.model.HealthPatternType
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.RecurrenceRule
import com.abhinavsirohi.kiwi.domain.model.SelfCareCategory
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.TaskPriority
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.OffsetDateTime

class SupabaseRestoreRemoteDataSource(
    private val clientResult: RemoteResult<SupabaseClient>,
    private val sessionProvider: SessionProvider = when (clientResult) {
        is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
        is RemoteResult.Failure -> object : SessionProvider {
            override fun currentSession(): RemoteResult<AuthenticatedSession> = clientResult
        }
    },
    private val rowsSource: RestoreRowsSource? = (clientResult as? RemoteResult.Success)?.value
        ?.let(::SupabaseRestoreRowsSource),
) : RestoreRemoteDataSource {
    override suspend fun fetchSnapshot(userId: String): RemoteResult<RestoreSnapshot> {
        val session = when (val result = sessionProvider.currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return result
        }
        if (session.userId != userId) return RemoteResult.Failure(RemoteError.AccessDenied)
        val remoteRows = rowsSource
            ?: return RemoteResult.Failure((clientResult as RemoteResult.Failure).error)

        return try {
            val profile = remoteRows.rows("profiles", "id", userId).singleOrNull()?.toProfile()
            RemoteResult.Success(
                RestoreSnapshot(
                    profile = profile,
                    tasks = remoteRows.rows("tasks", "user_id", userId).map(JsonObject::toTask),
                    subtasks = remoteRows.rows("subtasks", "user_id", userId).map(JsonObject::toSubtask),
                    cycleRecords = remoteRows.rows("cycle_records", "user_id", userId).map(JsonObject::toCycleRecord),
                    wellnessDailyRecords = remoteRows.rows("wellness_daily_records", "user_id", userId)
                        .map(JsonObject::toWellnessDailyRecord),
                    healthAlertEpisodes = remoteRows.rows("health_alert_episodes", "user_id", userId)
                        .map(JsonObject::toHealthAlertEpisode),
                    diaryEntries = remoteRows.rows("diary_entries", "user_id", userId).map(JsonObject::toDiaryEntry),
                    selfCareRoutines = remoteRows.rows("self_care_routines", "user_id", userId)
                        .map(JsonObject::toSelfCareRoutine),
                    taskPostponements = remoteRows.rows("task_postponements", "user_id", userId)
                        .map(JsonObject::toTaskPostponement),
                    weeklyReflections = remoteRows.rows("weekly_reflections", "user_id", userId)
                        .map(JsonObject::toWeeklyReflection),
                ),
            )
        } catch (throwable: Throwable) {
            RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        }
    }

}

fun interface RestoreRowsSource {
    suspend fun rows(table: String, ownerColumn: String, userId: String): List<JsonObject>
}

class SupabaseRestoreRowsSource(private val client: SupabaseClient) : RestoreRowsSource {
    override suspend fun rows(table: String, ownerColumn: String, userId: String): List<JsonObject> =
        client.postgrest[table].select {
            filter { eq(ownerColumn, userId) }
        }.decodeList()
}

private fun JsonObject.toProfile(): Profile = Profile(
    localId = requiredString("id"),
    userId = requiredString("id"),
    preferredName = requiredString("preferred_name"),
    createdAt = requiredTimestamp("created_at"),
    updatedAt = requiredTimestamp("updated_at"),
)

private fun JsonObject.toTask(): Task = Task(
    localId = requiredString("local_id"),
    title = requiredString("title"),
    description = optionalString("description"),
    category = enumValue(requiredString("category"), TaskCategory.entries),
    priority = enumValue(requiredString("priority"), TaskPriority.entries),
    notes = optionalString("notes"),
    scheduledDate = optionalString("scheduled_date"),
    scheduledTimeMinutes = optionalInt("scheduled_time_minutes"),
    recurrenceRule = RecurrenceRule(
        frequency = enumValue(requiredString("recurrence_frequency"), RecurrenceFrequency.entries),
        interval = requiredInt("recurrence_interval"),
        endDate = optionalString("recurrence_end_date"),
    ),
    recurrenceSeriesId = optionalString("recurrence_series_id"),
    isCompleted = requiredBoolean("is_completed"),
    position = requiredInt("position"),
    metadata = metadata(),
)

private fun JsonObject.toSubtask() = Subtask(
    localId = requiredString("local_id"), taskLocalId = requiredString("task_local_id"),
    title = requiredString("title"), isCompleted = requiredBoolean("is_completed"),
    position = requiredInt("position"), metadata = metadata(),
)

private fun JsonObject.toCycleRecord() = CycleRecord(
    localId = requiredString("local_id"), startDate = requiredString("start_date"),
    endDate = optionalString("end_date"), metadata = metadata(),
)

private fun JsonObject.toWellnessDailyRecord() = WellnessDailyRecord(
    localId = requiredString("local_id"), recordDate = requiredString("record_date"),
    cycleLocalId = optionalString("cycle_local_id"),
    flow = optionalString("flow")?.let { enumValue(it, WellnessFlow.entries) },
    painLevel = optionalInt("pain_level"), crampsLevel = optionalInt("cramps_level"),
    symptoms = optionalString("symptoms_json")?.let(::decodeJsonStringList).orEmpty(),
    mood = optionalString("mood"), energyLevel = optionalInt("energy_level"),
    sleepMinutes = optionalInt("sleep_minutes"), notes = optionalString("notes"),
    exercise = optionalString("exercise"), selfCareMedicationNotes = optionalString("self_care_medication_notes"),
    metadata = metadata(),
)

private fun JsonObject.toHealthAlertEpisode() = HealthAlertEpisode(
    localId = requiredString("local_id"),
    patternType = enumValue(requiredString("pattern_type"), HealthPatternType.entries),
    sourceCycleId = requiredString("source_cycle_id"), evidenceCount = requiredInt("evidence_count"),
    firstEvidenceDate = requiredString("first_evidence_date"), lastEvidenceDate = requiredString("last_evidence_date"),
    state = enumValue(requiredString("state"), HealthAlertState.entries),
    notifiedAt = optionalLong("notified_at"), metadata = metadata(),
)

private fun JsonObject.toDiaryEntry() = DiaryEntry(
    localId = requiredString("local_id"), title = requiredString("title"), content = requiredString("content"),
    entryDate = requiredString("entry_date"), bestThing = optionalString("best_thing"),
    mood = optionalString("mood"), isFavourite = requiredBoolean("is_favourite"), metadata = metadata(),
)

private fun JsonObject.toSelfCareRoutine() = SelfCareRoutine(
    localId = requiredString("local_id"), name = requiredString("name"), description = optionalString("description"),
    category = enumValue(requiredString("category"), SelfCareCategory.entries),
    scheduledTimeMinutes = optionalInt("scheduled_time_minutes"),
    repeatDays = stringValue("repeat_days").split(',').filter(String::isNotBlank)
        .map { enumValue(it, SelfCareDay.entries) }.toSet(),
    checklist = decodeSeparatedList(stringValue("checklist")), isActive = requiredBoolean("is_active"),
    completionDates = decodeSeparatedList(stringValue("completion_dates")).toSet(), metadata = metadata(),
)

private fun JsonObject.toTaskPostponement() = TaskPostponement(
    localId = requiredString("local_id"), taskLocalId = requiredString("task_local_id"),
    taskTitle = requiredString("task_title"), previousDate = requiredString("previous_date"),
    newDate = requiredString("new_date"), postponedAt = requiredLong("postponed_at"), metadata = metadata(),
)

private fun JsonObject.toWeeklyReflection() = WeeklyReflection(
    localId = requiredString("local_id"), weekStart = requiredString("week_start"),
    content = requiredString("content"), metadata = metadata(),
)

private fun JsonObject.metadata() = RecordMetadata(
    remoteId = requiredString("local_id"), userId = requiredString("user_id"),
    createdAt = requiredLong("created_at"), updatedAt = requiredLong("updated_at"),
    deletedAt = optionalLong("deleted_at"), syncState = SyncState.Synced,
    lastSyncError = null, deviceId = requiredString("device_id"),
)

private fun JsonObject.requiredString(key: String): String =
    optionalString(key)?.takeIf(String::isNotBlank) ?: error("Missing $key")
private fun JsonObject.stringValue(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: error("Missing $key")
private fun JsonObject.optionalString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.requiredInt(key: String): Int = this[key]?.jsonPrimitive?.int ?: error("Missing $key")
private fun JsonObject.optionalInt(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
private fun JsonObject.requiredLong(key: String): Long = this[key]?.jsonPrimitive?.long ?: error("Missing $key")
private fun JsonObject.optionalLong(key: String): Long? = this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
private fun JsonObject.requiredBoolean(key: String): Boolean = this[key]?.jsonPrimitive?.boolean ?: error("Missing $key")
private fun JsonObject.requiredTimestamp(key: String): Long =
    OffsetDateTime.parse(requiredString(key)).toInstant().toEpochMilli()
private fun <T : Enum<T>> enumValue(value: String, entries: List<T>): T =
    entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: error("Unsupported value $value")
private fun decodeSeparatedList(value: String) = value.split('\u001f').map(String::trim).filter(String::isNotEmpty)
private fun decodeJsonStringList(value: String): List<String> = runCatching {
    kotlinx.serialization.json.Json.decodeFromString<List<String>>(value)
}.getOrDefault(emptyList())
