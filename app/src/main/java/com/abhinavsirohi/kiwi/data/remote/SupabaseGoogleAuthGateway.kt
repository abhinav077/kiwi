package com.abhinavsirohi.kiwi.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken

interface GoogleAuthGateway {
    suspend fun signIn(idToken: String, nonce: String): RemoteResult<AuthenticatedSession>
}

class SupabaseGoogleAuthGateway(
    private val clientResult: RemoteResult<SupabaseClient>,
) : GoogleAuthGateway {
    override suspend fun signIn(
        idToken: String,
        nonce: String,
    ): RemoteResult<AuthenticatedSession> {
        val client = when (clientResult) {
            is RemoteResult.Success -> clientResult.value
            is RemoteResult.Failure -> return clientResult
        }

        return try {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                this.nonce = nonce
            }
            SupabaseSessionProvider(client).currentSession()
        } catch (throwable: Throwable) {
            RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        }
    }
}
