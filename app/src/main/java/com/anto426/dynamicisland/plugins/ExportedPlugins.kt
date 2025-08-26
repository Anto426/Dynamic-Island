package com.anto426.dynamicisland.plugins

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.anto426.dynamicisland.model.packageName
import com.anto426.dynamicisland.plugins.battery.BatteryPlugin
import com.anto426.dynamicisland.plugins.media.MediaSessionPlugin
import com.anto426.dynamicisland.plugins.notification.NotificationPlugin

class ExportedPlugins {

	companion object {

		val permissions: SnapshotStateMap<String, PluginPermission> = mutableStateMapOf(
			Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS to object : PluginPermission(
				name = "Notification access",
				description = "Allow Dynamic Island to listen to notifications and display them",
				requestIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
			) { override fun checkPermission(context: Context) : Boolean {
					val contentResolver = context.contentResolver
					val enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
					val packageName = packageName
					return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName)
				} }
		)

		val plugins = arrayListOf(
			NotificationPlugin(),
			MediaSessionPlugin(),
			BatteryPlugin(),
		)

		fun setupPlugins(context: Context) {
			for (plugin in plugins) {
				plugin.permissions.forEach { permissionId ->
					val permission = permissions[permissionId] ?: return@forEach
					permission.granted.value = permission.checkPermission(context)
				}
				plugin.enabled.value = plugin.isPluginEnabled(context)
			}

			permissions.forEach { (_, permission) ->
				permission.granted.value = permission.checkPermission(context)
			}
		}

		fun getPlugin(pluginId: String): BasePlugin {
			return plugins.first { it.id == pluginId }
		}
	}
}