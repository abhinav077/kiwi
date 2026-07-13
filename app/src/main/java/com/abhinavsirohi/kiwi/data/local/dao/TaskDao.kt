package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertSubtask(subtask: SubtaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("SELECT * FROM tasks WHERE localId = :localId")
    suspend fun findTask(localId: String): TaskEntity?

    @Query("SELECT * FROM subtasks WHERE localId = :localId")
    suspend fun findSubtask(localId: String): SubtaskEntity?

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY position, created_at")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM subtasks " +
            "WHERE task_local_id = :taskLocalId AND deleted_at IS NULL " +
            "ORDER BY position, created_at",
    )
    fun observeActiveSubtasks(taskLocalId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM tasks WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getTasksAwaitingSync(): List<TaskEntity>

    @Query("SELECT * FROM subtasks WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getSubtasksAwaitingSync(): List<SubtaskEntity>
}
