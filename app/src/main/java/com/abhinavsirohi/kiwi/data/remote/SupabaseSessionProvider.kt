package com.abhinavsirohi.kiwi.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

data class AuthenticatedSession(
    val userId: String,
    val email: String?,
)

interface SessionProvider {
    fun currentSession(): RemoteResult<AuthenticatedSession>
}

class SupabaseSessionProvider(
    private val sessionSource: SessionSource,
) : SessionProvider {
    constructor(supabaseClient: SupabaseClient) : this(
        sessionSource = SessionSource {
            supabaseClient.auth.currentSessionOrNull()?.user?.let { user ->
                AuthenticatedSession(userId = user.id, email = user.email)
            }
        },
    )

    override fun currentSession(): RemoteResult<AuthenticatedSession> {
        val session = try {
            sessionSource.currentSession()
        } catch (throwable: Throwable) {
            return RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        } ?: return RemoteResult.Failure(RemoteError.AuthenticationRequired)

        return RemoteResult.Success(session)
    }
}

fun interface SessionSource {
    fun currentSession(): AuthenticatedSession?
}
