package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.TaskPostponementEntity
import com.abhinavsirohi.kiwi.data.local.entity.WeeklyReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Upsert
    suspend fun upsertPostponement(event: TaskPostponementEntity)

    @Query("SELECT * FROM task_postponements WHERE localId = :localId LIMIT 1")
    suspend fun findPostponement(localId: String): TaskPostponementEntity?

    @Query(
        "SELECT * FROM task_postponements WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY postponed_at DESC",
    )
    fun observePostponements(userId: String): Flow<List<TaskPostponementEntity>>

    @Upsert
    suspend fun upsertReflection(reflection: WeeklyReflectionEntity)

    @Query("SELECT * FROM weekly_reflections WHERE localId = :localId LIMIT 1")
    suspend fun findReflectionByLocalId(localId: String): WeeklyReflectionEntity?

    @Query(
        "SELECT * FROM weekly_reflections WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY week_start DESC",
    )
    fun observeReflections(userId: String): Flow<List<WeeklyReflectionEntity>>

    @Query(
        "SELECT * FROM weekly_reflections WHERE user_id = :userId AND week_start = :weekStart LIMIT 1",
    )
    suspend fun findReflection(userId: String, weekStart: String): WeeklyReflectionEntity?
}
