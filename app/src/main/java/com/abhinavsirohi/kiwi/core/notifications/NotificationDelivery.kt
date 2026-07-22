package com.abhinavsirohi.kiwi.core.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal fun postNotificationIfPermitted(
    context: Context,
    notificationId: Int,
    notification: Notification,
) {
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    if (!permissionGranted) return

    try {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
        // Permission can be revoked between the check and delivery.
    }
}
