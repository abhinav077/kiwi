package com.abhinavsirohi.kiwi.data.repository

import androidx.room.withTransaction
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.database.SyncMetadata
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.ProfileEntity
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient

class RoomProfileRepository(
    private val database: KiwiDatabase,
    private val sessionProvider: SessionProvider,
    private val deviceId: String,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ProfileRepository {
    constructor(
        database: KiwiDatabase,
        clientResult: RemoteResult<SupabaseClient>,
        deviceId: String,
        currentTimeMillis: () -> Long = System::currentTimeMillis,
    ) : this(
        database = database,
        sessionProvider = sessionProvider(clientResult),
        deviceId = deviceId,
        currentTimeMillis = currentTimeMillis,
    )

    override suspend fun savePreferredName(preferredName: String): AppResult<Profile> {
        val session = when (val result = sessionProvider.currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return AppResult.Failure(
                IllegalStateException("Authenticated session is required"),
            )
        }

        return try {
            val now = currentTimeMillis()
            val profile = database.withTransaction {
                val existing = database.profileDao().findByUserId(session.userId)
                val entity = ProfileEntity(
                    localId = existing?.localId ?: session.userId,
                    preferredName = preferredName,
                    syncMetadata = SyncMetadata(
                        remoteId = session.userId,
                        userId = session.userId,
                        createdAt = existing?.syncMetadata?.createdAt ?: now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                        deviceId = deviceId,
                    ),
                )
                database.profileDao().upsert(entity)
                database.pendingChangeDao().enqueue(
                    PendingChangeEntity(
                        queueId = "PROFILE:${entity.localId}",
                        recordType = SyncRecordType.PROFILE,
                        recordLocalId = entity.localId,
                        operation = SyncOperation.UPSERT,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                entity.toDomain()
            }
            AppResult.Success(profile)
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable)
        }
    }

    private fun ProfileEntity.toDomain(): Profile = Profile(
        localId = localId,
        userId = syncMetadata.userId,
        preferredName = preferredName,
        createdAt = syncMetadata.createdAt,
        updatedAt = syncMetadata.updatedAt,
    )

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
