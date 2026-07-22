package com.abhinavsirohi.kiwi.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.data.local.notificationContentIsHidden

interface TaskReminderScheduler {
    fun schedule(task: Task)
    fun cancel(taskLocalId: String)
}

object NoOpTaskReminderScheduler : TaskReminderScheduler {
    override fun schedule(task: Task) = Unit
    override fun cancel(taskLocalId: String) = Unit
}

class AlarmManagerTaskReminderScheduler(
    private val context: Context,
    private val planner: TaskReminderPlanner = TaskReminderPlanner(),
) : TaskReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: Task) {
        val reminder = planner.reminderFor(task) ?: run {
            cancel(task.localId)
            return
        }
        TaskReminderNotifications.createChannel(context)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerAtMillis,
            reminderPendingIntent(context, reminder.taskLocalId),
        )
    }

    override fun cancel(taskLocalId: String) {
        alarmManager.cancel(reminderPendingIntent(context, taskLocalId))
    }
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskLocalId = intent.getStringExtra(EXTRA_TASK_LOCAL_ID) ?: return
        TaskReminderNotifications.createChannel(context)
        val content = TaskReminderPlanner().notificationContent()
        val notification = NotificationCompat.Builder(context, TaskReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(if (notificationContentIsHidden(context)) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        postNotificationIfPermitted(context, taskLocalId.hashCode(), notification)
    }
}

object TaskReminderNotifications {
    const val CHANNEL_ID = "task_reminders"
    private const val CHANNEL_NAME = "Task reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Private reminders for planned tasks"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

private const val ACTION_TASK_REMINDER = "com.abhinavsirohi.kiwi.TASK_REMINDER"
private const val EXTRA_TASK_LOCAL_ID = "task_local_id"

private fun reminderPendingIntent(context: Context, taskLocalId: String): PendingIntent = PendingIntent.getBroadcast(
    context,
    taskLocalId.hashCode(),
    Intent(context, TaskReminderReceiver::class.java)
        .setAction(ACTION_TASK_REMINDER)
        .putExtra(EXTRA_TASK_LOCAL_ID, taskLocalId)
        .setData(android.net.Uri.parse("kiwi://task-reminder/$taskLocalId")),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
