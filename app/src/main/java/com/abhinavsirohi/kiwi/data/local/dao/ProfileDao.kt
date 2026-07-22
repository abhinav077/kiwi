package com.abhinavsirohi.kiwi.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhinavsirohi.kiwi.data.local.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE user_id = :userId LIMIT 1")
    suspend fun findByUserId(userId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE localId = :localId LIMIT 1")
    suspend fun findByLocalId(localId: String): ProfileEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM profiles LIMIT 1)")
    suspend fun hasAnyProfile(): Boolean

    @Query("SELECT * FROM profiles WHERE sync_status != 'SYNCED' ORDER BY updated_at")
    suspend fun getAwaitingSync(): List<ProfileEntity>
}
