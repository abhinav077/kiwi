package com.abhinavsirohi.kiwi.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.database.SyncStatus
import com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SelfCareCategory
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.SyncState
import androidx.core.app.NotificationCompat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import com.abhinavsirohi.kiwi.data.local.notificationContentIsHidden

interface SelfCareReminderScheduler {
    fun schedule(routine: SelfCareRoutine)
    fun cancel(routineLocalId: String)
}

object NoOpSelfCareReminderScheduler : SelfCareReminderScheduler {
    override fun schedule(routine: SelfCareRoutine) = Unit
    override fun cancel(routineLocalId: String) = Unit
}

class AlarmManagerSelfCareReminderScheduler(
    private val context: Context,
    private val planner: SelfCareReminderPlanner = SelfCareReminderPlanner(),
) : SelfCareReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(routine: SelfCareRoutine) {
        val planned = planner.plan(routine) ?: run { cancel(routine.localId); return }
        SelfCareReminderNotifications.createChannel(context)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, planned.triggerAtMillis, pendingIntent(context, routine.localId))
    }

    override fun cancel(routineLocalId: String) { alarmManager.cancel(pendingIntent(context, routineLocalId)) }
}

data class PlannedSelfCareReminder(val routineLocalId: String, val triggerAtMillis: Long)

class SelfCareReminderPlanner(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun plan(routine: SelfCareRoutine): PlannedSelfCareReminder? {
        if (!routine.isActive || routine.scheduledTimeMinutes == null) return null
        val now = java.time.Instant.ofEpochMilli(nowMillis()).atZone(zoneId)
        val time = LocalTime.of(routine.scheduledTimeMinutes / 60, routine.scheduledTimeMinutes % 60)
        val days = routine.repeatDays.map(SelfCareDay::toDayOfWeek).toSet()
        val candidate = (0..7).asSequence()
            .map { now.toLocalDate().plusDays(it.toLong()) }
            .firstOrNull { date -> (days.isEmpty() || date.dayOfWeek in days) && date.atTime(time).atZone(zoneId).toInstant().toEpochMilli() > nowMillis() }
            ?: return null
        return PlannedSelfCareReminder(routine.localId, candidate.atTime(time).atZone(zoneId).toInstant().toEpochMilli())
    }
}

class SelfCareReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        SelfCareReminderNotifications.createChannel(context)
        val notification = NotificationCompat.Builder(context, SelfCareReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kiwi self-care reminder")
            .setContentText("A small moment for yourself is waiting.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(if (notificationContentIsHidden(context)) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        postNotificationIfPermitted(context, routineId.hashCode(), notification)
    }
}

class SelfCareReminderReconciliationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = runCatching {
        val application = applicationContext as KiwiApplication
        application.database.selfCareDao().getReminderCandidates()
            .map(SelfCareRoutineEntity::toReminderRoutine)
            .forEach(application.selfCareReminderScheduler::schedule)
        Result.success()
    }.getOrElse { Result.retry() }
}

class SelfCareReminderReconciliationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACTIONS) {
            SelfCareReminderReconciliationWorkRequest.enqueue(context)
        }
    }

    private companion object {
        val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED)
    }
}

object SelfCareReminderReconciliationWorkRequest {
    const val UNIQUE_WORK_NAME = "kiwi-self-care-reminder-reconciliation"

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SelfCareReminderReconciliationWorker>().build(),
        )
    }
}

object SelfCareReminderNotifications {
    const val CHANNEL_ID = "self_care_reminders"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Self-care reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Private reminders for self-care routines"
            },
        )
    }
}

private const val EXTRA_ROUTINE_ID = "routine_id"
private fun pendingIntent(context: Context, routineId: String) = PendingIntent.getBroadcast(
    context,
    routineId.hashCode(),
    Intent(context, SelfCareReminderReceiver::class.java)
        .setAction("com.abhinavsirohi.kiwi.SELF_CARE_REMINDER")
        .setData(android.net.Uri.parse("kiwi://self-care-reminder/$routineId"))
        .putExtra(EXTRA_ROUTINE_ID, routineId),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun SelfCareDay.toDayOfWeek() = when (this) {
    SelfCareDay.Monday -> DayOfWeek.MONDAY
    SelfCareDay.Tuesday -> DayOfWeek.TUESDAY
    SelfCareDay.Wednesday -> DayOfWeek.WEDNESDAY
    SelfCareDay.Thursday -> DayOfWeek.THURSDAY
    SelfCareDay.Friday -> DayOfWeek.FRIDAY
    SelfCareDay.Saturday -> DayOfWeek.SATURDAY
    SelfCareDay.Sunday -> DayOfWeek.SUNDAY
}

private fun SelfCareRoutineEntity.toReminderRoutine() = SelfCareRoutine(
    localId = localId,
    name = name,
    description = description,
    category = SelfCareCategory.entries.firstOrNull { it.name == category } ?: SelfCareCategory.Mind,
    scheduledTimeMinutes = scheduledTimeMinutes,
    repeatDays = repeatDays.split(',').mapNotNull { value -> SelfCareDay.entries.firstOrNull { it.name == value } }.toSet(),
    checklist = emptyList(),
    isActive = isActive,
    completionDates = emptySet(),
    metadata = RecordMetadata(
        remoteId = syncMetadata.remoteId, userId = syncMetadata.userId, createdAt = syncMetadata.createdAt,
        updatedAt = syncMetadata.updatedAt, deletedAt = syncMetadata.deletedAt,
        syncState = when (syncMetadata.syncStatus) { SyncStatus.PENDING -> SyncState.Pending; SyncStatus.SYNCED -> SyncState.Synced; SyncStatus.FAILED -> SyncState.Failed },
        lastSyncError = syncMetadata.lastSyncError, deviceId = syncMetadata.deviceId,
    ),
)
