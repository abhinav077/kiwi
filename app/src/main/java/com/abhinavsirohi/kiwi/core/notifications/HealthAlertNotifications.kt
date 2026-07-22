package com.abhinavsirohi.kiwi.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import com.abhinavsirohi.kiwi.data.local.notificationContentIsHidden

data class PlannedHealthAlertNotification(val episodeId: String, val triggerAtMillis: Long)

class HealthAlertNotificationPlanner(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun plan(episodeId: String): PlannedHealthAlertNotification {
        val now = Instant.ofEpochMilli(nowMillis()).atZone(zoneId)
        val time = now.toLocalTime()
        val trigger = if (time >= QUIET_START || time < QUIET_END) {
            val day = if (time >= QUIET_START) now.toLocalDate().plusDays(1) else now.toLocalDate()
            day.atTime(QUIET_END).atZone(zoneId).toInstant().toEpochMilli()
        } else nowMillis()
        return PlannedHealthAlertNotification(episodeId, trigger)
    }

    companion object {
        val QUIET_START: LocalTime = LocalTime.of(21, 0)
        val QUIET_END: LocalTime = LocalTime.of(8, 0)
    }
}

fun interface HealthAlertNotificationScheduler { fun schedule(episode: HealthAlertEpisode) }

object NoOpHealthAlertNotificationScheduler : HealthAlertNotificationScheduler {
    override fun schedule(episode: HealthAlertEpisode) = Unit
}

class AlarmManagerHealthAlertNotificationScheduler(
    private val context: Context,
    private val planner: HealthAlertNotificationPlanner = HealthAlertNotificationPlanner(),
) : HealthAlertNotificationScheduler {
    override fun schedule(episode: HealthAlertEpisode) {
        val planned = planner.plan(episode.localId)
        HealthAlertNotifications.createChannel(context)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            planned.triggerAtMillis,
            healthAlertPendingIntent(context, episode.localId),
        )
    }
}

class HealthAlertNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID) ?: return
        HealthAlertNotifications.createChannel(context)
        val notification = NotificationCompat.Builder(context, HealthAlertNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kiwi wellness note")
            .setContentText("A recorded pattern may be worth reviewing in Kiwi.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(if (notificationContentIsHidden(context)) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        postNotificationIfPermitted(context, episodeId.hashCode(), notification)
    }
}

object HealthAlertNotifications {
    const val CHANNEL_ID = "wellness_cautions"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Wellness cautions", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Private, non-diagnostic notes about recorded wellness patterns"
            },
        )
    }
}

private const val EXTRA_EPISODE_ID = "episode_id"
private fun healthAlertPendingIntent(context: Context, episodeId: String) = PendingIntent.getBroadcast(
    context,
    episodeId.hashCode(),
    Intent(context, HealthAlertNotificationReceiver::class.java)
        .setAction("com.abhinavsirohi.kiwi.HEALTH_ALERT")
        .setData(android.net.Uri.parse("kiwi://health-alert/$episodeId"))
        .putExtra(EXTRA_EPISODE_ID, episodeId),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
