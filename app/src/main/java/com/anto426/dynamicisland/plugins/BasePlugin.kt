package com.anto426.dynamicisland.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.anto426.dynamicisland.model.SETTINGS_CHANGED
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.service.IslandOverlayService
import androidx.core.content.edit
import com.anto426.dynamicisland.plugins.PluginManager
import androidx.annotation.StringRes

abstract class BasePlugin {
	abstract val id: String
	abstract val name: String
	abstract val description: String
	abstract val author: String
	abstract val version : String
	abstract val sourceCodeUrl: Any
	abstract val permissions: ArrayList<String>
	abstract var enabled: MutableState<Boolean>
	abstract var pluginSettings: SnapshotStateMap<String, PluginSettingsItem>

	@StringRes
	open val nameRes: Int? = null
	@StringRes
	open val descriptionRes: Int? = null

	var priority: MutableState<PluginPriority> = androidx.compose.runtime.mutableStateOf(PluginPriority.MEDIUM)
	var lastUpdatedAt: MutableState<Long> = androidx.compose.runtime.mutableStateOf(0L)
	var wantsToShow: MutableState<Boolean> = androidx.compose.runtime.mutableStateOf(false)
	var isStarted: MutableState<Boolean> = androidx.compose.runtime.mutableStateOf(false)

	val active get() = enabled.value && allPermissionsGranted

	abstract fun canExpand(): Boolean

	abstract fun onCreate(context: IslandOverlayService?)
	@Composable
	abstract fun Composable()
	abstract fun onClick()
	abstract fun onDestroy()
	@Composable
	abstract fun PermissionsRequired()

	@Composable
	abstract fun LeftOpenedComposable()
	@Composable
	abstract fun RightOpenedComposable()

	abstract fun onRightSwipe()
	abstract fun onLeftSwipe()

	open fun initSettings(context: Context) {}

	open fun onSettingsChanged(context: Context, key: String, value: Any?) {}

	@SuppressLint("SuspiciousIndentation")
    fun switchEnabled(context: Context, enabled: Boolean = !this.enabled.value): Boolean {

		return if (allPermissionsGranted || !enabled) {
            context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit {
                putBoolean(id, enabled)
            }

			context.sendBroadcast(Intent(SETTINGS_CHANGED))
			this.enabled.value = enabled

			true
		} else {
			false
		}
	}

	val allPermissionsGranted: Boolean
		get() = permissions.all { permission ->
			ExportedPlugins.permissions[permission]?.granted?.value ?: false
		}

	fun isPluginEnabled(context: Context): Boolean {
		val preferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)
		return preferences.getBoolean(id, false)
	}

	fun persistPriority(context: Context) {
		context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit {
			putString("${id}_priority", priority.value.name)
		}
	}

	fun loadPriority(context: Context) {
		val preferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)
		priority.value = PluginPriority.fromString(preferences.getString("${id}_priority", null))
	}

	@Suppress("UNUSED_PARAMETER")
	fun show(service: IslandOverlayService, timeoutMs: Long = 0L) {
		wantsToShow.value = true
		lastUpdatedAt.value = System.currentTimeMillis()
		PluginManager.submit(this, priority.value, timeoutMs)
	}

	@Suppress("UNUSED_PARAMETER")
	fun hide(service: IslandOverlayService) {
		wantsToShow.value = false
		PluginManager.end(this)
	}
}