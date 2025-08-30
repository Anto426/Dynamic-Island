package com.anto426.dynamicisland.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Token
import androidx.compose.ui.graphics.vector.ImageVector
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.navigation.IslandDestination

// Settings Interface requires Int for resource IDs
interface SettingItem {
	@get:StringRes
	val title: Int
	@get:StringRes
	val subtitle: Int
	val icon: ImageVector
	val route: String
}

// Setting Objects now implement both interfaces correctly
object ThemeSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_theme
	override val subtitle: Int = R.string.settings_item_theme_subtitle
	override val icon: ImageVector = Icons.Default.DarkMode
	override val route: String = "theme"
}

object PositionSizeSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_position_size
	override val subtitle: Int = R.string.settings_item_position_size_subtitle
	override val icon: ImageVector = Icons.Default.Straighten
	override val route: String = "position_size"
}

object EnabledAppsSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_enabled_apps
	override val subtitle: Int = R.string.settings_item_enabled_apps_subtitle
	override val icon: ImageVector = Icons.Default.Apps
	override val route: String = "enabled_apps"
}

object BehaviorSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_behavior
	override val subtitle: Int = R.string.settings_item_behavior_subtitle
	override val icon: ImageVector = Icons.Default.Token
	override val route: String = "behavior"
}


object AboutSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_about
	override val subtitle: Int = R.string.settings_item_about_subtitle
	override val icon: ImageVector = Icons.Default.Info
	override val route: String = "about"
}

object DeveloperScreen : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_dev
	override val subtitle: Int = R.string.settings_item_dev_subtitle
	override val icon: ImageVector = Icons.Default.Adb
	override val route: String = "Developer"
}

object UpdateSetting : IslandDestination, SettingItem {
	override val title: Int = R.string.settings_item_updates
	override val subtitle: Int = R.string.settings_item_updates_subtitle
	override val icon: ImageVector = Icons.Default.SystemUpdate
	override val route: String = "updates"
}

val settings = listOf(
	ThemeSetting,
	BehaviorSetting,
	UpdateSetting,
	PositionSizeSetting,
	EnabledAppsSetting,
	AboutSetting,
	DeveloperScreen
)

// Theme settings - these can remain as raw strings as they are likely used directly in UI logic
val radioOptions = listOf(
	"System",
	"Light",
	"Dark",
)
