package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreDatabaseWriter
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreSnapshot
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreWriteResult
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
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomRestoreDatabaseWriter(
    private val database: KiwiDatabase,
) : RestoreDatabaseWriter {
    override suspend fun applySnapshot(snapshot: RestoreSnapshot): RestoreWriteResult =
        database.withTransaction {
            var restoredRecords = 0
            snapshot.profile?.let { incoming ->
                val local = database.profileDao().findByUserId(incoming.userId)
                if (local == null || incoming.updatedAt > local.syncMetadata.updatedAt) {
                    database.profileDao().upsert(
                        ProfileEntity(
                            localId = incoming.localId,
                            preferredName = incoming.preferredName,
                            syncMetadata = SyncMetadata(
                                remoteId = incoming.userId,
                                userId = incoming.userId,
                                createdAt = incoming.createdAt,
                                updatedAt = incoming.updatedAt,
                                syncStatus = SyncStatus.SYNCED,
                                deviceId = RESTORE_DEVICE_ID,
                            ),
                        ),
                    )
                    restoredRecords += 1
                }
            }

            val tasks = snapshot.tasks.newerThanLocal(database.taskDao()::findTask) { it.metadata.updatedAt }
            database.taskDao().upsertTasks(tasks.map { it.toEntity() })
            val subtasks = snapshot.subtasks.newerThanLocal(database.taskDao()::findSubtask) { it.metadata.updatedAt }
            database.taskDao().upsertSubtasks(subtasks.map { it.toEntity() })

            val cycles = snapshot.cycleRecords.newerThanLocal(database.wellnessDao()::findCycleRecord) { it.metadata.updatedAt }
            cycles.forEach { database.wellnessDao().upsertCycleRecord(it.toEntity()) }
            val daily = snapshot.wellnessDailyRecords.newerThanLocal(database.wellnessDao()::findDailyRecord) { it.metadata.updatedAt }
            daily.forEach { database.wellnessDao().upsertDailyRecord(it.toEntity()) }
            val alerts = snapshot.healthAlertEpisodes.newerThanLocal(database.healthAlertDao()::find) { it.metadata.updatedAt }
            alerts.forEach { database.healthAlertDao().upsert(it.toEntity()) }
            val diary = snapshot.diaryEntries.newerThanLocal(database.diaryDao()::find) { it.metadata.updatedAt }
            diary.forEach { database.diaryDao().upsert(it.toEntity()) }
            val routines = snapshot.selfCareRoutines.newerThanLocal(database.selfCareDao()::find) { it.metadata.updatedAt }
            routines.forEach { database.selfCareDao().upsert(it.toEntity()) }
            val postponements = snapshot.taskPostponements.newerThanLocal(database.reviewDao()::findPostponement) { it.metadata.updatedAt }
            postponements.forEach { database.reviewDao().upsertPostponement(it.toEntity()) }
            val reflections = snapshot.weeklyReflections.newerThanLocal(database.reviewDao()::findReflectionByLocalId) { it.metadata.updatedAt }
            reflections.forEach { database.reviewDao().upsertReflection(it.toEntity()) }

            restoredRecords += tasks.size + subtasks.size + cycles.size + daily.size + alerts.size +
                diary.size + routines.size + postponements.size + reflections.size
            RestoreWriteResult(tasks.size, subtasks.size, restoredRecords)
        }

    private suspend fun <T : Any, E : Any> List<T>.newerThanLocal(
        findLocal: suspend (String) -> E?,
        incomingUpdatedAt: (T) -> Long,
    ): List<T> = filter { incoming ->
        val local = findLocal(incoming.localId())
        local == null || incomingUpdatedAt(incoming) > local.updatedAt()
    }

    private fun Any.localId(): String = when (this) {
        is Task -> localId; is Subtask -> localId; is CycleRecord -> localId
        is WellnessDailyRecord -> localId; is HealthAlertEpisode -> localId; is DiaryEntry -> localId
        is SelfCareRoutine -> localId; is TaskPostponement -> localId; is WeeklyReflection -> localId
        else -> error("Unsupported restore record")
    }

    private fun Any.updatedAt(): Long = when (this) {
        is TaskEntity -> syncMetadata.updatedAt; is SubtaskEntity -> syncMetadata.updatedAt
        is CycleRecordEntity -> syncMetadata.updatedAt; is WellnessDailyRecordEntity -> syncMetadata.updatedAt
        is HealthAlertEpisodeEntity -> syncMetadata.updatedAt; is DiaryEntryEntity -> syncMetadata.updatedAt
        is SelfCareRoutineEntity -> syncMetadata.updatedAt; is TaskPostponementEntity -> syncMetadata.updatedAt
        is WeeklyReflectionEntity -> syncMetadata.updatedAt
        else -> error("Unsupported local restore record")
    }

    private fun Task.toEntity() = TaskEntity(
        localId, title, description, category.name.uppercase(), priority.name.uppercase(), notes,
        scheduledDate, scheduledTimeMinutes, recurrenceRule.frequency.name.uppercase(), recurrenceRule.interval,
        recurrenceRule.endDate, recurrenceSeriesId, isCompleted, position, metadata.toEntity(),
    )
    private fun Subtask.toEntity() = SubtaskEntity(localId, taskLocalId, title, isCompleted, position, metadata.toEntity())
    private fun CycleRecord.toEntity() = CycleRecordEntity(localId, startDate, endDate, metadata.toEntity())
    private fun WellnessDailyRecord.toEntity() = WellnessDailyRecordEntity(
        localId, recordDate, cycleLocalId, flow?.name, painLevel, crampsLevel,
        symptoms.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) }, mood, energyLevel,
        sleepMinutes, notes, exercise, selfCareMedicationNotes, metadata.toEntity(),
    )
    private fun HealthAlertEpisode.toEntity() = HealthAlertEpisodeEntity(
        localId, patternType.name, sourceCycleId, evidenceCount, firstEvidenceDate, lastEvidenceDate,
        state.name, notifiedAt, metadata.toEntity(),
    )
    private fun DiaryEntry.toEntity() = DiaryEntryEntity(
        localId, title, content, entryDate, bestThing, mood, isFavourite, metadata.toEntity(),
    )
    private fun SelfCareRoutine.toEntity() = SelfCareRoutineEntity(
        localId, name, description, category.name, scheduledTimeMinutes,
        repeatDays.joinToString(",") { it.name }, checklist.joinToString(SEPARATOR), isActive,
        completionDates.joinToString(SEPARATOR), metadata.toEntity(),
    )
    private fun TaskPostponement.toEntity() = TaskPostponementEntity(
        localId, taskLocalId, taskTitle, previousDate, newDate, postponedAt, metadata.toEntity(),
    )
    private fun WeeklyReflection.toEntity() = WeeklyReflectionEntity(localId, weekStart, content, metadata.toEntity())

    private fun RecordMetadata.toEntity() = SyncMetadata(
        remoteId, userId, createdAt, updatedAt, deletedAt,
        when (syncState) {
            SyncState.Pending -> SyncStatus.PENDING
            SyncState.Synced -> SyncStatus.SYNCED
            SyncState.Failed -> SyncStatus.FAILED
        },
        lastSyncError, deviceId,
    )

    private companion object {
        const val RESTORE_DEVICE_ID = "cloud-restore"
        const val SEPARATOR = "\u001f"
    }
}
