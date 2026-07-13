package com.abhinavsirohi.kiwi

import android.app.Application
import android.provider.Settings
import androidx.room.Room
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.notifications.AlarmManagerTaskReminderScheduler
import com.abhinavsirohi.kiwi.core.notifications.AlarmManagerHealthAlertNotificationScheduler
import com.abhinavsirohi.kiwi.data.remote.KiwiSupabaseClient
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SupabaseConfiguration
import io.github.jan.supabase.SupabaseClient

class KiwiApplication : Application() {
    val database: KiwiDatabase by lazy {
        Room.databaseBuilder(this, KiwiDatabase::class.java, KiwiDatabase.DATABASE_NAME)
            .addMigrations(*KiwiDatabase.MIGRATIONS)
            .build()
    }

    val deviceId: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }

    val supabaseClient: RemoteResult<SupabaseClient> by lazy {
        KiwiSupabaseClient.create(SupabaseConfiguration.fromBuildConfig())
    }

    val taskReminderScheduler by lazy { AlarmManagerTaskReminderScheduler(this) }
    val healthAlertNotificationScheduler by lazy { AlarmManagerHealthAlertNotificationScheduler(this) }
}
