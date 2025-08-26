package com.anto426.dynamicisland

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.model.*
import com.anto426.dynamicisland.navigation.*
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.ui.settings.settings
import com.anto426.dynamicisland.ui.theme.DynamicIslandTheme
import com.anto426.dynamicisland.ui.theme.Theme

class MainActivity : ComponentActivity() {

	private lateinit var settingsPreferences: SharedPreferences

	companion object {
		lateinit var instance: MainActivity
	}

	var actions = mutableStateListOf<@Composable () -> Unit>()

	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		instance = this

		settingsPreferences = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)
		WindowCompat.setDecorFitsSystemWindows(window, false)

		invertTheme(true) // Invert theme on start

		setContent {
			val context = LocalContext.current

			// Setup plugins
			ExportedPlugins.setupPlugins(context)

			Theme.instance.Init()
			IslandSettings.instance.loadSettings(this)

			val disclosureAccepted by remember {
				mutableStateOf(settingsPreferences.getBoolean(DISCLOSURE_ACCEPTED, false))
			}

			if (!disclosureAccepted) {
				startActivity(Intent(this, DisclosureActivity::class.java))
				finish()
			}

			DynamicIslandTheme(darkTheme = Theme.instance.isDarkTheme) {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background
				) {
					val navController = rememberNavController()
					val currentBackStack by navController.currentBackStackEntryAsState()
					val currentDestination = currentBackStack?.destination
					val settingsRoutes = settings.map { (it as IslandDestination).route }

					val currentScreen: IslandDestination =
						(bottomDestinations.find { it.route == currentDestination?.route }
							?: settings.find { (it as IslandDestination).route == currentDestination?.route }
							?: if (currentDestination?.route == IslandPluginSettings.routeWithArgs) IslandPluginSettings else IslandHome) as IslandDestination

					LaunchedEffect(currentScreen) { actions.clear() }

					val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

					Scaffold(
						topBar = {
							CenterAlignedTopAppBar(
								scrollBehavior = scrollBehavior,
								colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
									containerColor = MaterialTheme.colorScheme.surface
								),
								navigationIcon = {
									val showBack = currentDestination?.route in settingsRoutes
											|| currentDestination?.route == IslandPluginSettings.routeWithArgs
									if (showBack) {
										IconButton(onClick = { navController.popBackStack() }) {
											Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
										}
									} else {
										Spacer(modifier = Modifier.width(48.dp)) // balance space
									}
								},
								title = {
									Box(modifier = Modifier.fillMaxWidth()) {
										Crossfade(targetState = if (currentScreen == IslandHome) stringResource(R.string.app_name) else currentScreen.title) { text ->
											Text(
												text = text,
												style = MaterialTheme.typography.titleLarge,
												modifier = Modifier.align(Alignment.Center),
												textAlign = TextAlign.Center
											)
										}
									}
								},
								actions = {
									Row { actions.forEach { it() } }
								}
							)
						},
						bottomBar = {
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.padding(bottom = 24.dp),
								contentAlignment = Alignment.Center
							) {
								NavigationBar(
									modifier = Modifier
										.clip(RoundedCornerShape(32.dp))
										.shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp))
										.align(Alignment.Center),
									containerColor = MaterialTheme.colorScheme.surface,
									contentColor = MaterialTheme.colorScheme.onSurface
								) {
									bottomDestinations.forEach { destination ->
										val selected = currentScreen == destination
												|| (destination == IslandSettings && settings.contains(currentScreen))
												|| (destination == IslandPlugins && currentScreen == IslandPluginSettings)
										val scale by animateFloatAsState(targetValue = if (selected) 1.2f else 1f)

										NavigationBarItem(
											icon = {
												Icon(
													destination.icon,
													contentDescription = null,
													modifier = Modifier
														.size(22.dp)
														.graphicsLayer { scaleX = scale; scaleY = scale }
												)
											},
											label = { Text(destination.title, style = MaterialTheme.typography.labelSmall) },
											selected = selected,
											onClick = { navController.navigateSingleTopTo(destination.route) },
											colors = NavigationBarItemDefaults.colors(
												indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
												selectedIconColor = MaterialTheme.colorScheme.primary,
												selectedTextColor = MaterialTheme.colorScheme.primary,
												unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
												unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
											)
										)
									}
								}
							}
						}
					) { paddingValues ->
						IslandNavHost(
							modifier = Modifier
								.padding(paddingValues)
								.fillMaxSize(),
							navController = navController
						)
					}
				}
			}
		}
	}

	private fun invertTheme(invert: Boolean) {
		settingsPreferences.edit { putBoolean(THEME_INVERTED, invert) }
		sendBroadcast(Intent(SETTINGS_THEME_INVERTED))
	}

	override fun onResume() { super.onResume(); invertTheme(true) }
	override fun onPause() { super.onPause(); invertTheme(false) }
	override fun onStop() { super.onStop(); invertTheme(false) }
	override fun onDestroy() { super.onDestroy(); invertTheme(false) }
}
