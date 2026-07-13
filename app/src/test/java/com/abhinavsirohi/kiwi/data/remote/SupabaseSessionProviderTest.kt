package com.abhinavsirohi.kiwi.data.remote

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseSessionProviderTest {
    @Test
    fun authenticatedSession_exposesIdentityWithoutTokens() {
        val expected = AuthenticatedSession(userId = "user-1", email = "approved@example.com")
        val provider = SupabaseSessionProvider(SessionSource { expected })

        assertEquals(RemoteResult.Success(expected), provider.currentSession())
    }

    @Test
    fun missingSession_requiresAuthentication() {
        val provider = SupabaseSessionProvider(SessionSource { null })

        assertEquals(
            RemoteResult.Failure(RemoteError.AuthenticationRequired),
            provider.currentSession(),
        )
    }

    @Test
    fun sessionStorageFailure_isMappedSafely() {
        val provider = SupabaseSessionProvider(
            SessionSource { throw IOException("private session detail") },
        )

        assertEquals(
            RemoteResult.Failure(RemoteError.NetworkUnavailable),
            provider.currentSession(),
        )
    }
}
