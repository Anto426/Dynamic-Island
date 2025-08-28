package com.anto426.dynamicisland.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.ui.settings.pages.PluginSettingsScreen
import com.anto426.dynamicisland.ui.home.HomeScreen
import com.anto426.dynamicisland.ui.plugins.PluginScreen
import com.anto426.dynamicisland.ui.settings.*
import com.anto426.dynamicisland.ui.settings.pages.*


@Composable
fun IslandNavHost(
	modifier: Modifier = Modifier,
	navController: NavHostController,
) {
	NavHost(
		navController = navController,
		startDestination = bottomDestinations.first().route,
		modifier = modifier,
	) {
		composable(
			route = IslandHome.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			HomeScreen(
				onGetStartedClick = {
					navController.navigateSingleTopTo(IslandPlugins.route)
				},
				onShowDisclosureClick = {
					navController.navigate(AboutSetting.route)
				},
			)
		}
		composable(
			route = IslandPlugins.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			PluginScreen(
				onPluginClicked = { plugin ->
					navController.navigateToPluginSettings(plugin.id)
				}
			)
		}
		composable(
			route = IslandSettings.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			SettingsScreen(
				onSettingClicked = { setting ->
					navController.navigate(setting.route)
				}
			)
		}
		composable(
			route = ThemeSetting.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			ThemeSettingsScreen()
		}
		composable(
			route = BehaviorSetting.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			BehaviorSettingsScreen()
		}
		composable(
			route = PositionSizeSetting.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			PositionSizeSettingsScreen()
		}
		composable(
			route = EnabledAppsSetting.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			EnabledAppsSettingsScreen()
		}
		composable(
			route = AboutSetting.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			AboutSettingsScreen()
		}
		composable(
			route = DeveloperScreen.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			DeveloperScreen()
		}
		composable(
			route = IslandAdvancedSettings.route,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) {
			AdvancedSettingsScreen()
		}
		composable(
			route = IslandPluginSettings.routeWithArgs,
			arguments = IslandPluginSettings.arguments,
			deepLinks = IslandPluginSettings.deepLinks,
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) { backStackEntry ->
			val pluginId = backStackEntry.arguments?.getString(IslandPluginSettings.pluginArg)
			if (pluginId != null) {
				PluginSettingsScreen(
					plugin = ExportedPlugins.getPlugin(pluginId)
				)
			}
		}
	}
}

fun NavHostController.navigateSingleTopTo(route: String) =
	this.navigate(route) {
		popUpTo(
			this@navigateSingleTopTo.graph.findStartDestination().id
		) {
			saveState = true
		}
		launchSingleTop = true
		restoreState = true
	}

fun NavHostController.navigateToPluginSettings(pluginId: String) {
	this.navigate(route = "${IslandPluginSettings.route}/$pluginId") {
		popUpTo(IslandSettings.route) {
			saveState = true
		}
		launchSingleTop = true
	}
}

fun AnimatedContentTransitionScope<*>.slideInFromEnd() =
	slideInHorizontally(
		initialOffsetX = { fullWidth -> fullWidth },
		animationSpec = tween(300)
	) + fadeIn(animationSpec = tween(300))

fun AnimatedContentTransitionScope<*>.slideOutToStart() =
	slideOutHorizontally(
		targetOffsetX = { fullWidth -> -fullWidth },
		animationSpec = tween(300)
	) + fadeOut(animationSpec = tween(300))

fun AnimatedContentTransitionScope<*>.slideInFromStart() =
	slideInHorizontally(
		initialOffsetX = { fullWidth -> -fullWidth },
		animationSpec = tween(300)
	) + fadeIn(animationSpec = tween(300))

fun AnimatedContentTransitionScope<*>.slideOutToEnd() =
	slideOutHorizontally(
		targetOffsetX = { fullWidth -> fullWidth },
		animationSpec = tween(300)
	) + fadeOut(animationSpec = tween(300))