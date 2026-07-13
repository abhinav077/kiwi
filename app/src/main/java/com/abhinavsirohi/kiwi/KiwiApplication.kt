package com.abhinavsirohi.kiwi

import android.app.Application
import com.abhinavsirohi.kiwi.data.remote.KiwiSupabaseClient
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SupabaseConfiguration
import io.github.jan.supabase.SupabaseClient

class KiwiApplication : Application() {
    val supabaseClient: RemoteResult<SupabaseClient> by lazy {
        KiwiSupabaseClient.create(SupabaseConfiguration.fromBuildConfig())
    }
}
