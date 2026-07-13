package com.abhinavsirohi.kiwi.data.repository

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteErrorMapper
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface ApprovedUserAccess {
    data class Approved(val session: AuthenticatedSession) : ApprovedUserAccess
    data object Denied : ApprovedUserAccess
}

interface ApprovedUserRepository {
    suspend fun checkAccess(): RemoteResult<ApprovedUserAccess>
    suspend fun signOut(): RemoteResult<Unit>
}

class SupabaseApprovedUserRepository(
    private val clientResult: RemoteResult<SupabaseClient>,
) : ApprovedUserRepository {
    override suspend fun checkAccess(): RemoteResult<ApprovedUserAccess> {
        val client = when (clientResult) {
            is RemoteResult.Success -> clientResult.value
            is RemoteResult.Failure -> return clientResult
        }
        val session = when (val result = SupabaseSessionProvider(client).currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return result
        }
        val email = session.email?.trim()?.takeIf(String::isNotEmpty)
            ?: return RemoteResult.Failure(RemoteError.AuthenticationRequired)

        return try {
            val record = client.from("allowed_users")
                .select {
                    filter {
                        eq("email", email)
                        eq("is_active", true)
                    }
                }
                .decodeSingleOrNull<AllowedUserRecord>()
            RemoteResult.Success(evaluateApprovedUser(session, record))
        } catch (throwable: Throwable) {
            RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        }
    }

    override suspend fun signOut(): RemoteResult<Unit> {
        val client = when (clientResult) {
            is RemoteResult.Success -> clientResult.value
            is RemoteResult.Failure -> return clientResult
        }
        return try {
            client.auth.signOut()
            RemoteResult.Success(Unit)
        } catch (throwable: Throwable) {
            RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        }
    }
}

@Serializable
internal data class AllowedUserRecord(
    val email: String,
    @SerialName("is_active") val isActive: Boolean,
)

internal fun evaluateApprovedUser(
    session: AuthenticatedSession,
    record: AllowedUserRecord?,
): ApprovedUserAccess {
    val sessionEmail = session.email?.trim()
    val matchesSession = sessionEmail != null && record?.email
        ?.trim()
        ?.equals(sessionEmail, ignoreCase = true) == true
    return if (matchesSession && record?.isActive == true) {
        ApprovedUserAccess.Approved(session)
    } else {
        ApprovedUserAccess.Denied
    }
}
