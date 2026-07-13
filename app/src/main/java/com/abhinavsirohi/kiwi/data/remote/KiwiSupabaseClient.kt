package com.abhinavsirohi.kiwi.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object KiwiSupabaseClient {
    fun create(configuration: SupabaseConfiguration): RemoteResult<SupabaseClient> {
        val validation = configuration.validate()
        if (validation is RemoteResult.Failure) return validation

        return try {
            RemoteResult.Success(
                createSupabaseClient(
                    supabaseUrl = configuration.url,
                    supabaseKey = configuration.publishableKey,
                ) {
                    install(Auth)
                    install(Postgrest)
                },
            )
        } catch (throwable: Throwable) {
            RemoteResult.Failure(RemoteErrorMapper.map(throwable))
        }
    }
}
