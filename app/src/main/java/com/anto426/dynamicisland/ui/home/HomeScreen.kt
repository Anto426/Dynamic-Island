package com.anto426.dynamicisland.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.anto426.dynamicisland.model.BATTERY_OPTIMIZATION_DISMISSED
import com.anto426.dynamicisland.model.DISCLOSURE_ACCEPTED
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.packageName
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.ui.settings.pages.SettingsDivider

@Composable
fun HomeScreen(
	onGetStartedClick: () -> Unit,
	onShowDisclosureClick: () -> Unit,
) {
	val context = LocalContext.current
	val settingsPreferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)

	var optimizationDismissed by remember { mutableStateOf(settingsPreferences.getBoolean(BATTERY_OPTIMIZATION_DISMISSED, false)) }
	var disclosureAccepted by remember { mutableStateOf(settingsPreferences.getBoolean(DISCLOSURE_ACCEPTED, false)) }

	val celebrateComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebrate))
	var isCelebrating by remember { mutableStateOf(false) }

	var isOverlayGranted by remember { mutableStateOf(canDrawOverlays(context)) }
	var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(IslandOverlayService::class.java, context)) }

	val startForPermissionResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		isAccessibilityGranted = isAccessibilityServiceEnabled(IslandOverlayService::class.java, context)
		isOverlayGranted = canDrawOverlays(context)

		if (isAccessibilityGranted && isOverlayGranted) {
			isCelebrating = true
			Handler(Looper.getMainLooper()).postDelayed({
				isCelebrating = false
			}, celebrateComposition?.duration?.toLong() ?: 0)
		}
	}

	fun switchAccessibilityService() {
		if (!isAccessibilityGranted) {
			startForPermissionResult.launch(
				Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
			)
		} else {
			IslandOverlayService.getInstance()?.disableSelf()
			isAccessibilityGranted = false
		}
	}

	val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
	var isIgnoringBatteryOptimizations by remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(packageName)) }
	val startForBatteryOptimizationResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		isIgnoringBatteryOptimizations = !powerManager.isIgnoringBatteryOptimizations(packageName)
	}

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.animateContentSize(),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		contentPadding = PaddingValues(16.dp)
	) {
		// Card che raggruppa lo stato dei servizi e i permessi
		item {
			Card(
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					ServiceStatusCard(
						isAccessibilityGranted = isAccessibilityGranted,
						switchAccessibility = { switchAccessibilityService() }
					)
					SettingsDivider()
					PermissionItem(
						title = "Permesso di Sovrapposizione",
						description = "Mostra l'isola sopra le altre app",
						icon = Icons.Default.Layers,
						checked = isOverlayGranted,
						onClick = {
							startForPermissionResult.launch(
								Intent(
									Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
									Uri.parse("package:$packageName")
								)
							)
						}
					)
					SettingsDivider()
					PermissionItem(
						title = "Permesso di Accessibilità",
						description = "Necessario per interagire con le app",
						icon = Icons.Default.SettingsAccessibility,
						checked = isAccessibilityGranted,
						onClick = { switchAccessibilityService() }
					)
				}
			}
		}

		// Altre schede
		item {
			AnimatedVisibility(
				visible = !disclosureAccepted,
				enter = fadeIn() + expandVertically(),
				exit = fadeOut() + shrinkVertically()
			) {
				DisclosureCard(
					onAcceptClick = {
						disclosureAccepted = true
						settingsPreferences.edit().putBoolean(DISCLOSURE_ACCEPTED, true).apply()
					},
					onShowClick = onShowDisclosureClick
				)
			}
		}

		item {
			AnimatedVisibility(
				visible = ExportedPlugins.plugins.all { !it.active },
				enter = fadeIn() + expandVertically(),
				exit = fadeOut() + shrinkVertically()
			) {
				NoPluginsActivatedCard(
					onGetStartedClick = onGetStartedClick
				)
			}
		}

		item {
			AnimatedVisibility(
				visible = isIgnoringBatteryOptimizations && !optimizationDismissed,
				enter = fadeIn() + expandVertically(),
				exit = fadeOut() + shrinkVertically()
			) {
				OptimizationCard(
					startForResult = startForBatteryOptimizationResult,
					onDismiss = {
						optimizationDismissed = true
						settingsPreferences.edit().putBoolean(BATTERY_OPTIMIZATION_DISMISSED, true).apply()
					}
				)
			}
		}
	}

	LottieAnimation(
		composition = celebrateComposition,
		isPlaying = isCelebrating,
		restartOnPlay = true,
		contentScale = ContentScale.Crop
	)
}

@Composable
fun DisclosureCard(
	onAcceptClick: () -> Unit,
	onShowClick: () -> Unit,
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(Icons.Rounded.Policy, contentDescription = null, modifier = Modifier.size(24.dp))
				Spacer(modifier = Modifier.width(16.dp))
				Text("Informativa", style = MaterialTheme.typography.titleLarge)
			}
			Text(
				"Utilizzando questa app, accetti i termini e le condizioni dell'app e dei plugin che utilizzi.",
				style = MaterialTheme.typography.bodyMedium
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(onClick = onShowClick) {
					Text("Leggi")
				}
				Spacer(modifier = Modifier.width(8.dp))
				Button(onClick = onAcceptClick) {
					Text("Ho capito")
				}
			}
		}
	}
}

@Composable
fun NoPluginsActivatedCard(
	onGetStartedClick: () -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(Icons.Rounded.ExtensionOff, contentDescription = null, modifier = Modifier.size(24.dp))
				Spacer(modifier = Modifier.width(16.dp))
				Text("Nessun plugin attivo", style = MaterialTheme.typography.titleLarge)
			}
			Text(
				"Per usare Dynamic Island, devi attivare almeno un plugin. Vai alla pagina dei plugin per iniziare.",
				style = MaterialTheme.typography.bodyMedium
			)
			Button(
				onClick = onGetStartedClick,
				modifier = Modifier.align(Alignment.End)
			) {
				Text("Inizia")
			}
		}
	}
}

@Composable
fun ServiceStatusCard(
	isAccessibilityGranted: Boolean,
	switchAccessibility: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.extraLarge)
			.clickable { switchAccessibility() },
		colors = CardDefaults.cardColors(
			containerColor = animateColorAsState(
				targetValue = if (isAccessibilityGranted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
			).value
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			val composition by rememberLottieComposition(
				spec = LottieCompositionSpec.RawRes(if (isAccessibilityGranted) R.raw.service_enabled else R.raw.service_disabled)
			)
			LottieAnimation(
				composition = composition,
				iterations = LottieConstants.IterateForever,
				contentScale = ContentScale.Fit,
				modifier = Modifier.size(64.dp)
			)

			Spacer(modifier = Modifier.width(16.dp))

			Column(
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = if (isAccessibilityGranted) "SERVIZIO ATTIVO" else "SERVIZIO DISABILITATO",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
					letterSpacing = 2.sp
				)
				Text(
					text = "Tocca per ${if (isAccessibilityGranted) "disattivare" else "attivare"}",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
fun OptimizationCard(
	startForResult: ManagedActivityResultLauncher<Intent, ActivityResult>,
	onDismiss: () -> Unit
) {
	val context = LocalContext.current
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(24.dp))
				Spacer(modifier = Modifier.width(16.dp))
				Text("Ottimizzazione batteria", style = MaterialTheme.typography.titleLarge)
			}
			Text(
				"Per evitare che l'app venga terminata dal sistema, disabilita l'ottimizzazione della batteria per Dynamic Island.",
				style = MaterialTheme.typography.bodyMedium
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(onClick = onDismiss) {
					Text("Ignora")
				}
				Spacer(modifier = Modifier.width(8.dp))
				Button(onClick = {
					startForResult.launch(
						Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
							data = Uri.parse("package:${context.packageName}")
						}
					)
				}) {
					Text("Disabilita ottimizzazione")
				}
			}
		}
	}
}

private fun isAccessibilityServiceEnabled(accessibilityService: Class<*>?, context: Context): Boolean {
	val expectedComponentName = ComponentName(context, accessibilityService!!)
	val enabledServicesSetting =
		Settings.Secure.getString(
			context.contentResolver,
			Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
		) ?: return false
	val colonSplitter = TextUtils.SimpleStringSplitter(':')
	colonSplitter.setString(enabledServicesSetting)
	while (colonSplitter.hasNext()) {
		val componentNameString = colonSplitter.next()
		val enabledService = ComponentName.unflattenFromString(componentNameString)
		if (enabledService != null && enabledService == expectedComponentName) return true
	}
	return false
}

private fun canDrawOverlays(context: Context): Boolean { return Settings.canDrawOverlays(context) }

@Composable
fun PermissionItem(
	title: String,
	description: String,
	icon: ImageVector,
	checked: Boolean,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.clickable(onClick = onClick)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(24.dp)
		)
		Spacer(modifier = Modifier.width(16.dp))
		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(text = title, style = MaterialTheme.typography.titleMedium)
			Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		Switch(
			checked = checked,
			onCheckedChange = { onClick() }
		)
	}
}