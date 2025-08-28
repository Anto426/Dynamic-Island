package com.anto426.dynamicisland.island

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anto426.dynamicisland.model.*
import androidx.core.content.edit

class IslandSettings {

	companion object {
		val instance = IslandSettings()
	}

	var positionX by mutableIntStateOf(0)
	var positionY by mutableIntStateOf(5)
	var width by mutableIntStateOf(150)
	var height by mutableIntStateOf(200)
	var cornerRadius by mutableIntStateOf(60)
	var gravity by mutableStateOf(IslandGravity.Center)

	var enabledApps = mutableStateListOf<String>()

	var showOnLockScreen by mutableStateOf(false)
	var showInLandscape by mutableStateOf(false)
	var showBorders by mutableStateOf(false)

	var autoHideOpenedAfter by mutableFloatStateOf(5000f)

	// Nuove impostazioni avanzate
	var animationsEnabled by mutableStateOf(true)
	var hapticFeedback by mutableStateOf(true)
	var soundEnabled by mutableStateOf(false)
	var autoHideExpandedAfter by mutableFloatStateOf(10000f)
	var dynamicThemeEnabled by mutableStateOf(false)
	var silentMode by mutableStateOf(false)
	var lowPowerMode by mutableStateOf(false)
	var notificationPriority by mutableIntStateOf(1) // 0: bassa, 1: normale, 2: alta

	fun applySettings(context: Context) {
		val settings = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)
		settings.edit {
            putInt(POSITION_X, positionX)
                .putInt(POSITION_Y, positionY)
                .putInt(SIZE_X, width)
                .putInt(SIZE_Y, height)
                .putInt(CORNER_RADIUS, cornerRadius)
                .putStringSet(ENABLED_APPS, enabledApps.toSet())
                .putBoolean(SHOW_ON_LOCK_SCREEN, showOnLockScreen)
                .putBoolean(SHOW_IN_LANDSCAPE, showInLandscape)
                .putFloat(AUTO_HIDE_OPENED_AFTER, autoHideOpenedAfter)
                .putBoolean(SHOW_BORDER, showBorders)
                .putString(GRAVITY, gravity.name)
                .putBoolean(ANIMATIONS_ENABLED, animationsEnabled)
                .putBoolean(HAPTIC_FEEDBACK, hapticFeedback)
                .putBoolean(SOUND_ENABLED, soundEnabled)
                .putFloat(AUTO_HIDE_EXPANDED_AFTER, autoHideExpandedAfter)
                .putBoolean(DYNAMIC_THEME_ENABLED, dynamicThemeEnabled)
                .putBoolean(SILENT_MODE, silentMode)
                .putBoolean(LOW_POWER_MODE, lowPowerMode)
                .putInt(NOTIFICATION_PRIORITY, notificationPriority)
        }
	}

	fun loadSettings(context: Context) {
		val settings = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)
		positionX = settings.getInt(POSITION_X, 0)
		positionY = settings.getInt(POSITION_Y, 5)
		width = settings.getInt(SIZE_X, 150)
		height = settings.getInt(SIZE_Y, 200)
		cornerRadius = settings.getInt(CORNER_RADIUS, 60)
		enabledApps.clear()
		enabledApps.addAll(settings.getStringSet(ENABLED_APPS, setOf()) ?: setOf())
		showOnLockScreen = settings.getBoolean(SHOW_ON_LOCK_SCREEN, false)
		showInLandscape = settings.getBoolean(SHOW_IN_LANDSCAPE, false)
		autoHideOpenedAfter = settings.getFloat(AUTO_HIDE_OPENED_AFTER, 5000f)
		showBorders = settings.getBoolean(SHOW_BORDER, false)
		gravity = IslandGravity.valueOf(settings.getString(GRAVITY, IslandGravity.Center.name) ?: IslandGravity.Center.name)

		// Carica nuove impostazioni avanzate
		animationsEnabled = settings.getBoolean(ANIMATIONS_ENABLED, true)
		hapticFeedback = settings.getBoolean(HAPTIC_FEEDBACK, true)
		soundEnabled = settings.getBoolean(SOUND_ENABLED, false)
		autoHideExpandedAfter = settings.getFloat(AUTO_HIDE_EXPANDED_AFTER, 10000f)
		dynamicThemeEnabled = settings.getBoolean(DYNAMIC_THEME_ENABLED, false)
		silentMode = settings.getBoolean(SILENT_MODE, false)
		lowPowerMode = settings.getBoolean(LOW_POWER_MODE, false)
		notificationPriority = settings.getInt(NOTIFICATION_PRIORITY, 1)
	}
}

enum class IslandGravity {
	Left,
	Right,
	Center
}