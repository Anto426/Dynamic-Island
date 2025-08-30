package com.anto426.dynamicisland.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.anto426.dynamicisland.MainActivity
import com.anto426.dynamicisland.R

object UpdateNotifications {
    private const val CHANNEL_ID = "updates_channel"
    private const val NOTIFICATION_ID = 2001

    fun showUpdateAvailableNotification(
        context: Context,
        version: String,
        downloadUrl: String,
        releaseNotes: String?
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_update_dialog", true)
            putExtra("new_version", version)
            putExtra("download_url", downloadUrl)
            putExtra("release_notes", releaseNotes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.update_available))
            .setContentText(context.getString(R.string.update_available_desc, version))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        (
                            context.getString(R.string.update_available_prompt, version) +
                                "\n\n" + (releaseNotes ?: context.getString(R.string.no_description))
                            )
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.update_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
