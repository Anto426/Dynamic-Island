package com.anto426.dynamicisland.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.anto426.dynamicisland.navigation.IslandDestination

// Settings
interface SettingItem {
	val title: String
	val subtitle: String
	val icon: ImageVector
	val route: String
}

object ThemeSetting : IslandDestination, SettingItem {
	override val title: String = "Theme"
	override val subtitle: String = "Change the theme of the app"
	override val icon: ImageVector = Icons.Default.DarkMode
	override val route: String = "theme"
}
object PositionSizeSetting : IslandDestination, SettingItem {
	override val title: String = "Position & Size"
	override val subtitle: String = "Change the position and size of the island"
	override val icon: ImageVector = Icons.Default.Straighten
	override val route: String = "position_size"
}
object EnabledAppsSetting : IslandDestination, SettingItem {
	override val title: String = "Enabled Apps"
	override val subtitle: String = "Change the apps that dynamically appear on the island"
	override val icon: ImageVector = Icons.Default.Apps
	override val route: String = "enabled_apps"
}
object BehaviorSetting : IslandDestination, SettingItem {
	override val title: String = "Behavior"
	override val subtitle: String = "Change the behavior of the island"
	override val icon: ImageVector = Icons.Default.Token
	override val route: String = "behavior"
}
object AboutSetting : IslandDestination, SettingItem {
	override val title: String = "About"
	override val subtitle: String = "About the app"
	override val icon: ImageVector = Icons.Default.Info
	override val route: String = "about"
}

object DeveloperScreen : IslandDestination, SettingItem {
	override val title: String = "Dev"
	override val subtitle: String = "About the Developer"
	override val icon: ImageVector = Icons.Default.Adb
	override val route: String = "Developer"
}



val settings = listOf(
	ThemeSetting,
	BehaviorSetting,
	PositionSizeSetting,
	EnabledAppsSetting,
	AboutSetting,
	DeveloperScreen
)

// Theme settings
val radioOptions = listOf(
	"System",
	"Light",
	"Dark",
)