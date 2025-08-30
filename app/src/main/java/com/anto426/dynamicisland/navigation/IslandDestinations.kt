package com.anto426.dynamicisland.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.anto426.dynamicisland.R

// Base interface for all destinations
interface IslandDestination {
	val icon: ImageVector
	val route: String
	// This MUST be an Int to match the SettingItem interface
	@get:StringRes
	val title: Int
}

// Main destinations
object IslandHome : IslandDestination {
	override val icon = Icons.Filled.Home
	override val route = "home"
	override val title = R.string.destination_home // Use resource ID
}

object IslandPlugins : IslandDestination {
	override val icon = Icons.Filled.Extension
	override val route = "plugins"
	override val title = R.string.destination_plugins // Use resource ID
}

object IslandSettings : IslandDestination {
	override val icon = Icons.Filled.Settings
	override val route = "settings"
	override val title = R.string.destination_settings // Use resource ID
}

object IslandAdvancedSettings : IslandDestination {
	override val icon = Icons.Filled.Build
	override val route = "advanced"
	override val title = R.string.destination_advanced // Use resource ID
}

// Destination for plugin settings with arguments and deep links
object IslandPluginSettings : IslandDestination {
	override val icon = Icons.Filled.Settings
	override val route = "settings_item"
	override val title = R.string.destination_plugin_settings // Use resource ID

	const val pluginArg = "plugin_id"
	val routeWithArgs = "$route/{$pluginArg}"
	val arguments = listOf(
		navArgument(pluginArg) { type = NavType.StringType }
	)
	val deepLinks = listOf(
		navDeepLink { uriPattern = "dynamicisland://$route/{$pluginArg}" }
	)
}

// List of the main destinations for the Bottom Navigation
val bottomDestinations = listOf(
	IslandHome,
	IslandPlugins,
	IslandSettings,
)

