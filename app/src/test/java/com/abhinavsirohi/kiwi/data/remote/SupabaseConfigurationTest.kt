package com.abhinavsirohi.kiwi.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseConfigurationTest {
    @Test
    fun missingValues_areRejectedWithoutEchoingCredentials() {
        val result = SupabaseConfiguration(url = "", publishableKey = "").validate()

        assertEquals(RemoteResult.Failure(RemoteError.ConfigurationMissing), result)
        assertEquals("Kiwi’s secure backup is not configured yet.", RemoteError.ConfigurationMissing.userMessage())
    }

    @Test
    fun invalidUrl_isRejected() {
        val result = SupabaseConfiguration(
            url = "http://not-secure.example.com",
            publishableKey = "public-key",
        ).validate()

        assertEquals(RemoteResult.Failure(RemoteError.ConfigurationInvalid), result)
    }

    @Test
    fun validPublicConfiguration_isAccepted() {
        val result = SupabaseConfiguration(
            url = "https://example.supabase.co",
            publishableKey = "public-key",
        ).validate()

        assertEquals(RemoteResult.Success(Unit), result)
    }
}
