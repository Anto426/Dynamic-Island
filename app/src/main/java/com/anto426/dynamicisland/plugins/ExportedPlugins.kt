package com.anto426.dynamicisland.plugins

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anto426.dynamicisland.model.packageName
import com.anto426.dynamicisland.plugins.battery.BatteryPlugin
import com.anto426.dynamicisland.plugins.media.MediaSessionPlugin
import com.anto426.dynamicisland.plugins.notification.NotificationPlugin
import com.anto426.dynamicisland.R

class ExportedPlugins {

	companion object {

		val permissions: SnapshotStateMap<String, PluginPermission> = mutableStateMapOf(
			Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
			) { override fun checkPermission(context: Context) : Boolean {
					// Check if NotificationService is running as a proxy for permission granted
					return com.anto426.dynamicisland.model.service.NotificationService.getInstance() != null
				} },
			Settings.ACTION_ACCESSIBILITY_SETTINGS to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean {
					val cr = context.contentResolver
					val enabled = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
					val services = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
					return enabled && (services?.contains(context.packageName) == true)
				} },
			Settings.ACTION_MANAGE_OVERLAY_PERMISSION to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
			) { override fun checkPermission(context: Context): Boolean { return Settings.canDrawOverlays(context) } },
			Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean {
					val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
					return pm.isIgnoringBatteryOptimizations(context.packageName)
				} },
			Manifest.permission.POST_NOTIFICATIONS to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean { return NotificationManagerCompat.from(context).areNotificationsEnabled() } },
			Manifest.permission.READ_MEDIA_IMAGES to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean { return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED } },
			Manifest.permission.READ_MEDIA_VIDEO to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean { return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED } },
			Manifest.permission.READ_MEDIA_AUDIO to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean { return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED } },
			Manifest.permission.REQUEST_INSTALL_PACKAGES to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
			) { override fun checkPermission(context: Context): Boolean { return context.packageManager.canRequestPackageInstalls() } },
			Manifest.permission.WRITE_EXTERNAL_STORAGE to object : PluginPermission(
				name = "",
				description = "",
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
			) { override fun checkPermission(context: Context): Boolean { return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED } }
		)

		val plugins = arrayListOf(
			NotificationPlugin(),
			MediaSessionPlugin(),
			BatteryPlugin(),
		)

		fun setupPlugins(context: Context) {
			// Ensure permission labels/descriptions are localized and intents are targeted to this package where relevant
			permissions[Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS]?.apply {
				name = context.getString(R.string.permission_notification_listener_title)
				description = context.getString(R.string.permission_notification_listener_desc)
			}
			permissions[Settings.ACTION_ACCESSIBILITY_SETTINGS]?.apply {
				name = context.getString(R.string.accessibility_permission_title)
				description = context.getString(R.string.accessibility_permission_description)
			}
			permissions[Settings.ACTION_MANAGE_OVERLAY_PERMISSION]?.apply {
				name = context.getString(R.string.overlay_permission_title)
				description = context.getString(R.string.overlay_permission_description)
				requestIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
			}
			permissions[Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS]?.apply {
				name = context.getString(R.string.permission_battery_title)
				description = context.getString(R.string.permission_battery_desc)
			}
			permissions[Manifest.permission.POST_NOTIFICATIONS]?.apply {
				name = context.getString(R.string.permission_post_notifications_title)
				description = context.getString(R.string.permission_post_notifications_desc)
				requestIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
					putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
				}
			}
			permissions[Manifest.permission.READ_MEDIA_IMAGES]?.apply {
				name = context.getString(R.string.permission_media_images_title)
				description = context.getString(R.string.permission_media_images_desc)
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
			}
			permissions[Manifest.permission.READ_MEDIA_VIDEO]?.apply {
				name = context.getString(R.string.permission_media_video_title)
				description = context.getString(R.string.permission_media_video_desc)
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
			}
			permissions[Manifest.permission.READ_MEDIA_AUDIO]?.apply {
				name = context.getString(R.string.permission_media_audio_title)
				description = context.getString(R.string.permission_media_audio_desc)
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
			}
			permissions[Manifest.permission.REQUEST_INSTALL_PACKAGES]?.apply {
				name = context.getString(R.string.permission_unknown_sources_title)
				description = context.getString(R.string.permission_unknown_sources_desc)
				requestIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
			}
			permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]?.apply {
				name = context.getString(R.string.permission_storage_legacy_title)
				description = context.getString(R.string.permission_storage_legacy_desc)
				requestIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
			}
			for (plugin in plugins) {
				// Initialize lightweight settings so the UI can render immediately
				plugin.initSettings(context)
				// Load persisted priority
				plugin.loadPriority(context)
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