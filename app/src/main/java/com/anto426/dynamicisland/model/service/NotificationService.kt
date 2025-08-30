package com.anto426.dynamicisland.model.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import com.anto426.dynamicisland.model.ACTION_CLOSE
import com.anto426.dynamicisland.model.ACTION_OPEN_CLOSE
import com.anto426.dynamicisland.model.NOTIFICATION_POSTED
import com.anto426.dynamicisland.model.NOTIFICATION_REMOVED
import com.anto426.dynamicisland.island.IslandSettings


class NotificationService : NotificationListenerService() {

	var notifications = mutableStateListOf<StatusBarNotification>()

	companion object {
		private var instance: NotificationService? = null

		fun getInstance(): NotificationService? {
			return instance
		}
	}

	private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {

			val statusBarNotification = notifications.firstOrNull { it.id == intent.getIntExtra("id", 0) } ?: return
			Log.d("NotificationService", "onReceive: ${statusBarNotification.id}, ${statusBarNotification.id}, ${statusBarNotification.notification.actions?.size}")
			val notification = statusBarNotification.notification

			if (intent.action == ACTION_OPEN_CLOSE) {
				// Logic to remove notification
				runCatching { notification.deleteIntent?.send() }.onFailure {
					cancelNotification(statusBarNotification.key)
				}

				// Start content intent from notification
				notification.contentIntent.send()
			}
			if (intent.action == ACTION_CLOSE) {
				// Logic to remove notification
				runCatching { notification.deleteIntent?.send() }.onFailure {
					cancelNotification(statusBarNotification.key)
				}
			}
		}
	}

	override fun onCreate() {
		super.onCreate()
		instance = this
		Log.d("NotificationService", "onCreate: ")

		// Register broadcast receiver (compat)
		ContextCompat.registerReceiver(
			this,
			mBroadcastReceiver,
			IntentFilter().apply {
				addAction(ACTION_OPEN_CLOSE)
				addAction(ACTION_CLOSE)
			},
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
	}

	override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
		super.onNotificationPosted(statusBarNotification)

	val notification = statusBarNotification.notification

		// Check if notification is in the enabled apps list
		if ((statusBarNotification.packageName !in IslandSettings.instance.enabledApps) && !IslandSettings.instance.enabledApps.isEmpty()) return

		Log.d("NotificationService", "Notification Category: ${notification.category}")
		// Ignore notifications from ->
		when (notification.category) {
			Notification.CATEGORY_SYSTEM, // Ignore system notifications
			Notification.CATEGORY_SERVICE, // Ignore service notifications
			Notification.CATEGORY_TRANSPORT, // Ignore media player controls notifications
			-> return
		}

		// Avoid duplicates by key
		if (notifications.none { it.key == statusBarNotification.key }) {
			notifications.add(statusBarNotification)
		} else {
			// Replace updated instance
			notifications.replaceAll { if (it.key == statusBarNotification.key) statusBarNotification else it }
		}
		Log.d("NotificationService", "Posted: $notifications")
		Log.d("NotificationService", "Posted: ${notifications.size}")

		sendBroadcast(Intent(NOTIFICATION_POSTED).apply {
			putExtra("id", statusBarNotification.id)
			putExtra("package_name", statusBarNotification.packageName)
			putExtra("category", notification.category)

			putExtra("time", statusBarNotification.postTime)
			// Do not pass large/small icons via broadcast to avoid binder limits; plugins can fetch from SBN

			val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
			val body = (
				notification.extras.getCharSequence(Notification.EXTRA_TEXT)
					?: notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
			)?.toString() ?: ""
			putExtra("title", title)
			putExtra("body", body)
		})
	}

	override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {

	// Remove notification from list by key for safety
	notifications.removeIf { it.key == statusBarNotification.key }
		Log.d("NotificationService", "Removed: $notifications")
		Log.d("NotificationService", "Latest notification: ${notifications.firstOrNull()}")

		// Send broadcast
		sendBroadcast(Intent(NOTIFICATION_REMOVED).apply {
			putExtra("id", statusBarNotification.id)
		})
	}

	override fun onDestroy() {
		super.onDestroy()
		instance = null
		unregisterReceiver(mBroadcastReceiver)
	}
}