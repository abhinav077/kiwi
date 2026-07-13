package com.abhinavsirohi.kiwi.data.remote

import com.abhinavsirohi.kiwi.BuildConfig

data class SupabaseConfiguration(
    val url: String,
    val publishableKey: String,
) {
    fun validate(): RemoteResult<Unit> {
        if (url.isBlank() || publishableKey.isBlank()) {
            return RemoteResult.Failure(RemoteError.ConfigurationMissing)
        }
        if (!url.startsWith("https://") || !url.endsWith(".supabase.co")) {
            return RemoteResult.Failure(RemoteError.ConfigurationInvalid)
        }
        return RemoteResult.Success(Unit)
    }

    companion object {
        fun fromBuildConfig(): SupabaseConfiguration = SupabaseConfiguration(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
    }
}
