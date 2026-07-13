package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.notifications.NoOpTaskReminderScheduler
import com.abhinavsirohi.kiwi.core.notifications.TaskReminderScheduler
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.RecurrenceRule
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.SyncState
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPriority
import com.abhinavsirohi.kiwi.domain.model.nextOccurrenceDate
import com.abhinavsirohi.kiwi.domain.model.validateRecurrenceRule
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() },
    private val reminderScheduler: TaskReminderScheduler = NoOpTaskReminderScheduler,
) : TaskRepository {
    constructor(
        database: KiwiDatabase,
        clientResult: RemoteResult<SupabaseClient>,
        deviceId: String,
        reminderScheduler: TaskReminderScheduler = NoOpTaskReminderScheduler,
    ) : this(database, sessionProvider(clientResult), deviceId, reminderScheduler = reminderScheduler)

    override fun observeTasks(): Flow<AppResult<List<Task>>> {
        val userId = authenticatedUserId() ?: return flowOf(
            AppResult.Failure(IllegalStateException("Authenticated session is required")),
        )
        return database.taskDao().observeActiveTasks(userId)
            .map<List<TaskEntity>, AppResult<List<Task>>> { entities ->
                AppResult.Success(entities.map { entity -> entity.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override fun observeSubtasks(taskLocalId: String): Flow<AppResult<List<Subtask>>> {
        val userId = authenticatedUserId() ?: return flowOf(
            AppResult.Failure(IllegalStateException("Authenticated session is required")),
        )
        return database.taskDao().observeActiveSubtasks(taskLocalId, userId)
            .map<List<SubtaskEntity>, AppResult<List<Subtask>>> { entities ->
                AppResult.Success(entities.map { entity -> entity.toDomain() })
            }
            .catch { emit(AppResult.Failure(it)) }
    }

    override fun observePlannerSyncState(): Flow<PlannerSyncState> = combine(
        database.pendingChangeDao().observePlannerPendingCount(),
        database.pendingChangeDao().observePlannerProcessingCount(),
        database.pendingChangeDao().observePlannerFailedCount(),
    ) { pending, processing, failed ->
        PlannerSyncState(pending, processing, failed)
    }

    override suspend fun createTask(task: NewTask): AppResult<Task> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        val rule = try {
            validateRecurrenceRule(task.scheduledDate, task.recurrenceRule)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        val now = currentTimeMillis()
        val localId = newLocalId()
        val entity = TaskEntity(
            localId = localId,
            title = task.title,
            description = task.description.cleaned(),
            category = task.category.name.uppercase(),
            priority = task.priority.name.uppercase(),
            notes = task.notes.cleaned(),
            scheduledDate = task.scheduledDate.cleaned(),
            scheduledTimeMinutes = task.scheduledTimeMinutes,
            recurrenceFrequency = rule.frequency.name.uppercase(),
            recurrenceInterval = rule.interval,
            recurrenceEndDate = rule.endDate,
            recurrenceSeriesId = localId.takeIf { rule.frequency != RecurrenceFrequency.None },
            syncMetadata = SyncMetadata(
                userId = userId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            ),
        )
        return persist(entity, SyncOperation.UPSERT).also { result ->
            if (result is AppResult.Success) scheduleReminder(result.value)
        }
    }

    override suspend fun createSubtask(subtask: NewSubtask): AppResult<Subtask> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        if (!ownsTask(subtask.taskLocalId, userId)) {
            return AppResult.Failure(IllegalAccessException("Task belongs to another user"))
        }
        val now = currentTimeMillis()
        val entity = SubtaskEntity(
            localId = newLocalId(),
            taskLocalId = subtask.taskLocalId,
            title = subtask.title,
            position = database.taskDao().nextSubtaskPosition(subtask.taskLocalId, userId),
            syncMetadata = SyncMetadata(userId = userId, createdAt = now, updatedAt = now, deviceId = deviceId),
        )
        return persistSubtask(entity, SyncOperation.UPSERT)
    }

    override suspend fun saveTask(task: Task): AppResult<Unit> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        if (task.title.isBlank()) return AppResult.Failure(IllegalArgumentException("Task title is required"))
        val rule = try {
            validateRecurrenceRule(task.scheduledDate, task.recurrenceRule)
        } catch (throwable: Throwable) {
            return AppResult.Failure(throwable)
        }
        return try {
            val existing = database.taskDao().findTask(task.localId)
                ?: return AppResult.Failure(IllegalArgumentException("Task was not found"))
            if (existing.syncMetadata.userId != userId || task.metadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Task belongs to another user"))
            }
            val now = currentTimeMillis()
            val updated = task.copy(recurrenceRule = rule).toEntity(
                metadata = existing.syncMetadata.copy(
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = SyncStatus.PENDING,
                    lastSyncError = null,
                    deviceId = deviceId,
                ),
            )
            var nextOccurrence: TaskEntity? = null
            database.withTransaction {
                database.taskDao().upsertTask(updated)
                enqueueTask(updated, SyncOperation.UPSERT)
                if (!existing.isCompleted && updated.isCompleted) {
                    nextOccurrence = generateNextOccurrence(updated, userId, now)
                }
            }
            scheduleReminder(updated.toDomain())
            nextOccurrence?.let { scheduleReminder(it.toDomain()) }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun tombstoneTask(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        return try {
            val existing = database.taskDao().findTask(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Task was not found"))
            if (existing.syncMetadata.userId != userId) {
                return AppResult.Failure(IllegalAccessException("Task belongs to another user"))
            }
            persist(
                existing.copy(
                    syncMetadata = existing.syncMetadata.copy(
                        updatedAt = deletedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.PENDING,
                        lastSyncError = null,
                        deviceId = deviceId,
                    ),
                ),
                SyncOperation.DELETE,
            ).asUnit().also { result ->
                if (result is AppResult.Success) reminderScheduler.cancel(localId)
            }
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun saveSubtask(subtask: Subtask): AppResult<Unit> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        if (subtask.title.isBlank()) return AppResult.Failure(IllegalArgumentException("Subtask title is required"))
        return try {
            val existing = database.taskDao().findSubtask(subtask.localId)
                ?: return AppResult.Failure(IllegalArgumentException("Subtask was not found"))
            if (existing.syncMetadata.userId != userId || existing.taskLocalId != subtask.taskLocalId ||
                !ownsTask(subtask.taskLocalId, userId)
            ) {
                return AppResult.Failure(IllegalAccessException("Subtask belongs to another user"))
            }
            persistSubtask(
                subtask.toEntity(
                    metadata = existing.syncMetadata.copy(
                        updatedAt = currentTimeMillis(),
                        deletedAt = null,
                        syncStatus = SyncStatus.PENDING,
                        lastSyncError = null,
                        deviceId = deviceId,
                    ),
                ),
                SyncOperation.UPSERT,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun tombstoneSubtask(localId: String, deletedAt: Long): AppResult<Unit> {
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        return try {
            val existing = database.taskDao().findSubtask(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Subtask was not found"))
            if (existing.syncMetadata.userId != userId || !ownsTask(existing.taskLocalId, userId)) {
                return AppResult.Failure(IllegalAccessException("Subtask belongs to another user"))
            }
            persistSubtask(
                existing.copy(
                    syncMetadata = existing.syncMetadata.copy(
                        updatedAt = deletedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.PENDING,
                        lastSyncError = null,
                        deviceId = deviceId,
                    ),
                ),
                SyncOperation.DELETE,
            ).asUnit()
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    override suspend fun moveSubtask(localId: String, direction: Int): AppResult<Unit> {
        if (direction != -1 && direction != 1) return AppResult.Failure(IllegalArgumentException("Invalid move direction"))
        val userId = authenticatedUserId()
            ?: return AppResult.Failure(IllegalStateException("Authenticated session is required"))
        return try {
            val current = database.taskDao().findSubtask(localId)
                ?: return AppResult.Failure(IllegalArgumentException("Subtask was not found"))
            if (current.syncMetadata.userId != userId || !ownsTask(current.taskLocalId, userId)) {
                return AppResult.Failure(IllegalAccessException("Subtask belongs to another user"))
            }
            val siblings = database.taskDao().getActiveSubtasks(current.taskLocalId, userId)
            val currentIndex = siblings.indexOfFirst { it.localId == localId }
            val targetIndex = currentIndex + direction
            if (currentIndex < 0 || targetIndex !in siblings.indices) return AppResult.Success(Unit)
            val reordered = siblings.toMutableList().apply {
                add(targetIndex, removeAt(currentIndex))
            }
            val now = currentTimeMillis()
            database.withTransaction {
                reordered.forEachIndexed { index, entity ->
                    val updated = entity.copy(
                        position = index,
                        syncMetadata = entity.syncMetadata.copy(
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            lastSyncError = null,
                            deviceId = deviceId,
                        ),
                    )
                    database.taskDao().upsertSubtask(updated)
                    enqueueSubtask(updated, SyncOperation.UPSERT)
                }
            }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private suspend fun persist(entity: TaskEntity, operation: SyncOperation): AppResult<Task> = try {
        database.withTransaction {
            database.taskDao().upsertTask(entity)
            enqueueTask(entity, operation)
        }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable)
    }

    private suspend fun generateNextOccurrence(completed: TaskEntity, userId: String, now: Long): TaskEntity? {
        val currentDate = completed.scheduledDate ?: return null
        val rule = completed.recurrenceRule()
        val nextDate = nextOccurrenceDate(currentDate, rule) ?: return null
        val seriesId = completed.recurrenceSeriesId ?: completed.localId
        if (database.taskDao().findOccurrence(seriesId, nextDate, userId) != null) return null

        val nextTaskId = newLocalId()
        val nextTask = completed.copy(
            localId = nextTaskId,
            scheduledDate = nextDate,
            recurrenceSeriesId = seriesId,
            isCompleted = false,
            syncMetadata = SyncMetadata(userId = userId, createdAt = now, updatedAt = now, deviceId = deviceId),
        )
        database.taskDao().upsertTask(nextTask)
        enqueueTask(nextTask, SyncOperation.UPSERT)

        database.taskDao().getActiveSubtasks(completed.localId, userId).forEach { source ->
            val copy = source.copy(
                localId = newLocalId(),
                taskLocalId = nextTaskId,
                isCompleted = false,
                syncMetadata = SyncMetadata(userId = userId, createdAt = now, updatedAt = now, deviceId = deviceId),
            )
            database.taskDao().upsertSubtask(copy)
            enqueueSubtask(copy, SyncOperation.UPSERT)
        }
        return nextTask
    }

    private suspend fun enqueueTask(entity: TaskEntity, operation: SyncOperation) {
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "TASK:${entity.localId}",
                recordType = SyncRecordType.TASK,
                recordLocalId = entity.localId,
                operation = operation,
                createdAt = entity.syncMetadata.createdAt,
                updatedAt = entity.syncMetadata.updatedAt,
            ),
        )
    }

    private suspend fun persistSubtask(entity: SubtaskEntity, operation: SyncOperation): AppResult<Subtask> = try {
        database.withTransaction {
            database.taskDao().upsertSubtask(entity)
            enqueueSubtask(entity, operation)
        }
        AppResult.Success(entity.toDomain())
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable)
    }

    private suspend fun enqueueSubtask(entity: SubtaskEntity, operation: SyncOperation) {
        database.pendingChangeDao().enqueue(
            PendingChangeEntity(
                queueId = "SUBTASK:${entity.localId}",
                recordType = SyncRecordType.SUBTASK,
                recordLocalId = entity.localId,
                operation = operation,
                createdAt = entity.syncMetadata.createdAt,
                updatedAt = entity.syncMetadata.updatedAt,
            ),
        )
    }

    private suspend fun ownsTask(taskLocalId: String, userId: String): Boolean =
        database.taskDao().findTask(taskLocalId)?.syncMetadata?.userId == userId

    private fun scheduleReminder(task: Task) {
        runCatching { reminderScheduler.schedule(task) }
    }

    private fun authenticatedUserId(): String? =
        (sessionProvider.currentSession() as? RemoteResult.Success)?.value?.userId

    private fun TaskEntity.toDomain() = Task(
        localId = localId,
        title = title,
        description = description,
        category = TaskCategory.entries.firstOrNull { it.name.equals(category, true) } ?: TaskCategory.Personal,
        priority = TaskPriority.entries.firstOrNull { it.name.equals(priority, true) } ?: TaskPriority.Normal,
        notes = notes,
        scheduledDate = scheduledDate,
        scheduledTimeMinutes = scheduledTimeMinutes,
        recurrenceRule = recurrenceRule(),
        recurrenceSeriesId = recurrenceSeriesId,
        isCompleted = isCompleted,
        position = position,
        metadata = syncMetadata.toDomain(),
    )

    private fun Task.toEntity(metadata: SyncMetadata) = TaskEntity(
        localId = localId,
        title = title.trim(),
        description = description.cleaned(),
        category = category.name.uppercase(),
        priority = priority.name.uppercase(),
        notes = notes.cleaned(),
        scheduledDate = scheduledDate.cleaned(),
        scheduledTimeMinutes = scheduledTimeMinutes,
        recurrenceFrequency = recurrenceRule.frequency.name.uppercase(),
        recurrenceInterval = recurrenceRule.interval,
        recurrenceEndDate = recurrenceRule.endDate.cleaned(),
        recurrenceSeriesId = if (recurrenceRule.frequency == RecurrenceFrequency.None) {
            null
        } else {
            recurrenceSeriesId ?: localId
        },
        isCompleted = isCompleted,
        position = position,
        syncMetadata = metadata,
    )

    private fun TaskEntity.recurrenceRule() = RecurrenceRule(
        frequency = RecurrenceFrequency.entries.firstOrNull { it.name.equals(recurrenceFrequency, true) }
            ?: RecurrenceFrequency.None,
        interval = recurrenceInterval,
        endDate = recurrenceEndDate,
    )

    private fun Subtask.toEntity(metadata: SyncMetadata) = SubtaskEntity(
        localId = localId,
        taskLocalId = taskLocalId,
        title = title.trim(),
        isCompleted = isCompleted,
        position = position,
        syncMetadata = metadata,
    )

    private fun SubtaskEntity.toDomain() = Subtask(
        localId = localId,
        taskLocalId = taskLocalId,
        title = title,
        isCompleted = isCompleted,
        position = position,
        metadata = syncMetadata.toDomain(),
    )

    private fun SyncMetadata.toDomain() = RecordMetadata(
        remoteId = remoteId,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = when (syncStatus) {
            SyncStatus.PENDING -> SyncState.Pending
            SyncStatus.SYNCED -> SyncState.Synced
            SyncStatus.FAILED -> SyncState.Failed
        },
        lastSyncError = lastSyncError,
        deviceId = deviceId,
    )

    private fun String?.cleaned(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private fun <T> AppResult<T>.asUnit(): AppResult<Unit> = when (this) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> this
    }

    private companion object {
        fun sessionProvider(clientResult: RemoteResult<SupabaseClient>): SessionProvider =
            when (clientResult) {
                is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
                is RemoteResult.Failure -> object : SessionProvider {
                    override fun currentSession() = clientResult
                }
            }
    }
}
