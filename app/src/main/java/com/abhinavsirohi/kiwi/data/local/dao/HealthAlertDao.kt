package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.HealthAlertEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthAlertDao {
    @Upsert suspend fun upsert(episode: HealthAlertEpisodeEntity)
    @Query("SELECT * FROM health_alert_episodes WHERE localId = :localId")
    suspend fun find(localId: String): HealthAlertEpisodeEntity?
    @Query("SELECT * FROM health_alert_episodes WHERE user_id = :userId AND deleted_at IS NULL ORDER BY updated_at DESC")
    suspend fun getAll(userId: String): List<HealthAlertEpisodeEntity>
    @Query("SELECT * FROM health_alert_episodes WHERE user_id = :userId AND deleted_at IS NULL AND state IN ('ACTIVE', 'ACKNOWLEDGED') ORDER BY updated_at DESC")
    fun observeVisible(userId: String): Flow<List<HealthAlertEpisodeEntity>>
}
