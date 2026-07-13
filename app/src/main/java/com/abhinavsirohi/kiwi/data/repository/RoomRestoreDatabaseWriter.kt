package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreDatabaseWriter
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreSnapshot
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreWriteResult
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task

class RoomRestoreDatabaseWriter(
    private val database: KiwiDatabase,
) : RestoreDatabaseWriter {
    override suspend fun applySnapshot(snapshot: RestoreSnapshot): RestoreWriteResult =
        database.withTransaction {
            val taskDao = database.taskDao()
            val tasksToRestore = snapshot.tasks.filter { incoming ->
                val local = taskDao.findTask(incoming.localId)
                local == null || incoming.metadata.updatedAt > local.syncMetadata.updatedAt
            }.map { it.toEntity() }
            taskDao.upsertTasks(tasksToRestore)

            val subtasksToRestore = snapshot.subtasks.filter { incoming ->
                val local = taskDao.findSubtask(incoming.localId)
                local == null || incoming.metadata.updatedAt > local.syncMetadata.updatedAt
            }.map { it.toEntity() }
            taskDao.upsertSubtasks(subtasksToRestore)

            RestoreWriteResult(
                restoredTasks = tasksToRestore.size,
                restoredSubtasks = subtasksToRestore.size,
            )
        }

    private fun Task.toEntity(): TaskEntity = TaskEntity(
        localId = localId,
        title = title,
        notes = notes,
        scheduledDate = scheduledDate,
        isCompleted = isCompleted,
        position = position,
        syncMetadata = metadata.toEntity(),
    )

    private fun Subtask.toEntity(): SubtaskEntity = SubtaskEntity(
        localId = localId,
        taskLocalId = taskLocalId,
        title = title,
        isCompleted = isCompleted,
        position = position,
        syncMetadata = metadata.toEntity(),
    )

    private fun RecordMetadata.toEntity(): SyncMetadata = SyncMetadata(
        remoteId = remoteId,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = when (syncState) {
            SyncState.Pending -> SyncStatus.PENDING
            SyncState.Synced -> SyncStatus.SYNCED
            SyncState.Failed -> SyncStatus.FAILED
        },
        lastSyncError = lastSyncError,
        deviceId = deviceId,
    )
}
