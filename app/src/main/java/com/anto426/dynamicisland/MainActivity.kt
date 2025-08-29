package com.anto426.dynamicisland

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anto426.dynamicisland.island.IslandSettings as IslandSettingsClass
import com.anto426.dynamicisland.model.*
import com.anto426.dynamicisland.navigation.*
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.ui.settings.settings
import com.anto426.dynamicisland.ui.theme.DynamicIslandTheme
import com.anto426.dynamicisland.ui.theme.Theme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anto426.dynamicisland.updater.UpdateManager
import com.anto426.dynamicisland.updater.UpdateViewModel

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

		// Gestisci gli intent per gli aggiornamenti
		handleUpdateIntent(intent)

		setContent {
			val context = LocalContext.current

			// Setup plugins
			ExportedPlugins.setupPlugins(context)

			Theme.instance.Init()
			IslandSettingsClass.instance.loadSettings(this)

			// Inizializza il sistema di aggiornamenti
			val updateManager = UpdateManager(this)
			if (updateManager.isAutoUpdateEnabled()) {
				updateManager.startPeriodicUpdateCheck()
				// Controlla aggiornamenti all'avvio se necessario
				updateManager.checkForUpdatesOnStartup()
			}

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

					// ViewModel per gli aggiornamenti
					val updateViewModel: UpdateViewModel = viewModel()
					val updateUiState by updateViewModel.uiState.collectAsState()

					// Inizializza il viewModel degli aggiornamenti
					LaunchedEffect(Unit) {
						updateViewModel.initialize(this@MainActivity)
					}

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
								colors = TopAppBarDefaults.topAppBarColors(
									containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.95f),
									titleContentColor = MaterialTheme.colorScheme.onSurface,
									navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
									actionIconContentColor = MaterialTheme.colorScheme.onSurface
								),
								navigationIcon = {
									val showBack = currentDestination?.route in settingsRoutes
											|| currentDestination?.route == IslandPluginSettings.routeWithArgs
									if (showBack) {
										IconButton(
											onClick = { navController.popBackStack() },
											colors = IconButtonDefaults.iconButtonColors(
												contentColor = MaterialTheme.colorScheme.onSurface
											)
										) {
											Icon(
												Icons.AutoMirrored.Filled.ArrowBack,
												contentDescription = "Indietro",
												modifier = Modifier.size(24.dp)
											)
										}
									}
								},
								title = {
									Box(modifier = Modifier.fillMaxWidth()) {
										Crossfade(
											targetState = if (currentScreen == IslandHome) stringResource(R.string.app_name) else currentScreen.title,
											animationSpec = tween(300)
										) { text ->
											Text(
												text = text,
												style = MaterialTheme.typography.titleLarge.copy(
													fontWeight = if (currentScreen == IslandHome) FontWeight.Bold else FontWeight.SemiBold
												),
												modifier = Modifier.align(Alignment.Center),
												textAlign = TextAlign.Center,
												maxLines = 1
											)
										}
									}
								},
								actions = {
									Row(
										horizontalArrangement = Arrangement.End,
										verticalAlignment = Alignment.CenterVertically
									) {
										actions.forEach { it() }
									}
								}
							)
						},
						bottomBar = {
							if (bottomDestinations.any { it.route == currentDestination?.route }) {
								Box(
									modifier = Modifier
										.fillMaxWidth()
										.padding(bottom = 24.dp),
									contentAlignment = Alignment.Center
								) {
									NavigationBar(
										modifier = Modifier
											.windowInsetsPadding(NavigationBarDefaults.windowInsets)
											.padding(horizontal = 16.dp, vertical = 8.dp)
											.clip(RoundedCornerShape(28.dp))
											.height(72.dp),
										tonalElevation = 8.dp,
										containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.95f),
										contentColor = MaterialTheme.colorScheme.onSurface
									) {
										bottomDestinations.forEach { destination ->
											val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

											val scale by animateFloatAsState(
												targetValue = if (selected) 1.15f else 1.0f,
												animationSpec = spring(
													dampingRatio = 0.5f,
													stiffness = 600f
												),
												label = "icon_scale_animation"
											)

											NavigationBarItem(
												icon = {
													Box {
														Icon(
															destination.icon,
															contentDescription = null,
															modifier = Modifier
																.size(if (selected) 28.dp else 24.dp)
																.graphicsLayer {
																	scaleX = scale
																	scaleY = scale
																}
														)
														// Badge per gli aggiornamenti disponibili
														if (destination.route == IslandSettings.route &&
															updateUiState.updateCheckState is UpdateViewModel.UpdateCheckState.UpdateAvailable) {
															Badge(
																containerColor = MaterialTheme.colorScheme.primary,
																contentColor = MaterialTheme.colorScheme.onPrimary,
																modifier = Modifier
																	.align(Alignment.TopEnd)
																	.offset(x = 6.dp, y = (-6).dp)
															) {
																Text("!", style = MaterialTheme.typography.labelSmall)
															}
														}
													}
												},
												label = {
													Text(
														destination.title,
														style = MaterialTheme.typography.labelSmall.copy(
															fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
														)
													)
												},
												selected = selected,
												onClick = {
													navController.navigate(destination.route) {
														popUpTo(navController.graph.findStartDestination().id) {
															saveState = true
														}
														launchSingleTop = true
														restoreState = true
													}
												},
												colors = NavigationBarItemDefaults.colors(
													indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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

	/**
	 * Gestisce gli intent per gli aggiornamenti
	 */
	private fun handleUpdateIntent(intent: Intent?) {
		intent?.let {
			val showUpdateDialog = it.getBooleanExtra("show_update_dialog", false)
			if (showUpdateDialog) {
				val newVersion = it.getStringExtra("new_version")
				val downloadUrl = it.getStringExtra("download_url")
				val releaseNotes = it.getStringExtra("release_notes")

				if (newVersion != null && downloadUrl != null) {
					showUpdateDialog(newVersion, downloadUrl, releaseNotes)
				}
			}
		}
	}

	/**
	 * Gestisce i nuovi intent quando l'app è già aperta
	 */
	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleUpdateIntent(intent)
	}

	/**
	 * Mostra il dialog per l'aggiornamento disponibile
	 */
	private fun showUpdateDialog(version: String, downloadUrl: String, releaseNotes: String?) {
		// TODO: Implementare il dialog di aggiornamento
		// Per ora mostriamo un toast con le informazioni disponibili
		val message = if (releaseNotes != null) {
			"Nuova versione disponibile: $version\n$releaseNotes"
		} else {
			"Nuova versione disponibile: $version"
		}

		android.widget.Toast.makeText(
			this,
			message,
			android.widget.Toast.LENGTH_LONG
		).show()
	}
}
