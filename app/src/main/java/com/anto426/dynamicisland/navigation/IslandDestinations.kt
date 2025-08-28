package com.anto426.dynamicisland.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

// Interfaccia base per tutte le destinazioni
interface IslandDestination {
	val icon: ImageVector
	val route: String
	val title: String
}

// Destinazioni principali
object IslandHome : IslandDestination {
	override val icon = Icons.Filled.Home
	override val route = "home"
	override val title = "Home"
}

object IslandPlugins : IslandDestination {
	override val icon = Icons.Filled.Extension
	override val route = "plugins"
	override val title = "Plugins"
}

object IslandSettings : IslandDestination {
	override val icon = Icons.Filled.Settings
	override val route = "settings"
	override val title = "Settings"
}

object IslandAdvancedSettings : IslandDestination {
	override val icon = Icons.Filled.Build
	override val route = "advanced"
	override val title = "Advanced"
}

// Destinazione per le impostazioni dei plugin con argomenti e deep link
object IslandPluginSettings : IslandDestination {
	override val icon = Icons.Filled.Settings
	override val route = "settings_item"
	override val title = "Plugin Settings"

	const val pluginArg = "plugin_id"

	val routeWithArgs = "$route/{$pluginArg}"

	val arguments = listOf(
		navArgument(pluginArg) { type = NavType.StringType }
	)

	val deepLinks = listOf(
		navDeepLink { uriPattern = "dynamicisland://$route/{$pluginArg}" }
	)
}

// Lista delle destinazioni principali per il Bottom Navigation
val bottomDestinations = listOf(
	IslandHome,
	IslandPlugins,
	IslandSettings,
)
