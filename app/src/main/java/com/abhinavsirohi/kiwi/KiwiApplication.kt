package com.abhinavsirohi.kiwi

import android.app.Application
import android.provider.Settings
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import com.abhinavsirohi.kiwi.core.notifications.AlarmManagerTaskReminderScheduler
import com.abhinavsirohi.kiwi.core.notifications.AlarmManagerHealthAlertNotificationScheduler
import com.abhinavsirohi.kiwi.data.remote.KiwiSupabaseClient
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SupabaseConfiguration
import io.github.jan.supabase.SupabaseClient
import com.abhinavsirohi.kiwi.data.local.AndroidDiaryPhotoLocalStore
import com.abhinavsirohi.kiwi.core.sync.WorkManagerDiaryPhotoSyncScheduler
import com.abhinavsirohi.kiwi.core.notifications.AlarmManagerSelfCareReminderScheduler
import com.abhinavsirohi.kiwi.data.local.KiwiSettingsStore
import com.abhinavsirohi.kiwi.core.sync.SyncWorkRequest
import com.abhinavsirohi.kiwi.core.sync.SyncWorkerFactory
import com.abhinavsirohi.kiwi.data.remote.SupabasePendingChangeProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class KiwiApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settingsStore by lazy { KiwiSettingsStore(this) }

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

    private val pendingChangeProcessor by lazy {
        SupabasePendingChangeProcessor(database, supabaseClient)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(pendingChangeProcessor))
            .build()

    val taskReminderScheduler by lazy { AlarmManagerTaskReminderScheduler(this) }
    val healthAlertNotificationScheduler by lazy { AlarmManagerHealthAlertNotificationScheduler(this) }
    val diaryPhotoLocalStore by lazy { AndroidDiaryPhotoLocalStore(this) }
    val diaryPhotoSyncScheduler by lazy { WorkManagerDiaryPhotoSyncScheduler(this) }
    val selfCareReminderScheduler by lazy { AlarmManagerSelfCareReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            database.pendingChangeDao().observeGenericPendingCount()
                .distinctUntilChanged()
                .filter { it > 0 }
                .collect {
                    SyncWorkRequest.enqueue(WorkManager.getInstance(this@KiwiApplication))
                }
        }
    }
}
