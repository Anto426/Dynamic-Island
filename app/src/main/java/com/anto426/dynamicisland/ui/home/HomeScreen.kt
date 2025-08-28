package com.anto426.dynamicisland.ui.home

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.core.net.toUri
import androidx.core.content.edit

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

	val allPermissionsGranted = isAccessibilityGranted && isOverlayGranted

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
		verticalArrangement = Arrangement.spacedBy(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
	) {
		// Header con stato dell'app
		item {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(16.dp)
			) {
				// Icona dell'app con badge di stato
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier.size(80.dp)
				) {
					Icon(
						imageVector = Icons.Rounded.Android,
						contentDescription = null,
						modifier = Modifier.size(64.dp),
						tint = MaterialTheme.colorScheme.primary
					)

					// Badge di stato
					val allPermissionsGranted = isAccessibilityGranted && isOverlayGranted
					if (allPermissionsGranted) {
						Icon(
							imageVector = Icons.Default.CheckCircle,
							contentDescription = "Configurato",
							modifier = Modifier
								.size(24.dp)
								.align(Alignment.BottomEnd),
							tint = MaterialTheme.colorScheme.primary
						)
					}
				}

				Text(
					text = stringResource(R.string.app_name),
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface
				)

				Text(
					text = if (allPermissionsGranted) "App configurata e pronta!" else "Configura i permessi per iniziare",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)

				// Indicatore di progresso
				if (!allPermissionsGranted) {
					LinearProgressIndicator(
					progress = { ((if (isOverlayGranted) 1 else 0) + (if (isAccessibilityGranted) 1 else 0)) / 2f },
					modifier = Modifier
												.fillMaxWidth(0.8f)
												.height(4.dp)
												.clip(RoundedCornerShape(2.dp)),
					color = MaterialTheme.colorScheme.primary,
					trackColor = MaterialTheme.colorScheme.surfaceVariant,
					strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
					)
				}
			}
		}

		// Card principale dei permessi con design migliorato
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerLow
				),
				elevation = CardDefaults.cardElevation(
					defaultElevation = 4.dp
				),
				shape = MaterialTheme.shapes.extraLarge
			) {
				Column(
					modifier = Modifier.padding(24.dp),
					verticalArrangement = Arrangement.spacedBy(24.dp)
				) {
					Text(
						text = "Permessi richiesti",
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.SemiBold,
						color = MaterialTheme.colorScheme.onSurface
					)

					ServiceStatusCard(
						isAccessibilityGranted = isAccessibilityGranted,
						switchAccessibility = { switchAccessibilityService() }
					)

					Divider(color = MaterialTheme.colorScheme.outlineVariant)

					PermissionItem(
						title = stringResource(R.string.overlay_permission_title),
						description = stringResource(R.string.overlay_permission_description),
						icon = Icons.Default.Layers,
						checked = isOverlayGranted,
						onClick = {
							startForPermissionResult.launch(
								Intent(
									Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
									"package:$packageName".toUri()
								)
							)
						}
					)

					Divider(color = MaterialTheme.colorScheme.outlineVariant)

					PermissionItem(
						title = stringResource(R.string.accessibility_permission_title),
						description = stringResource(R.string.accessibility_permission_description),
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
						settingsPreferences.edit { putBoolean(DISCLOSURE_ACCEPTED, true) }
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
						settingsPreferences.edit {
							putBoolean(BATTERY_OPTIMIZATION_DISMISSED, true)
						}
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
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 4.dp
		)
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					Icons.Rounded.Policy,
					contentDescription = null,
					modifier = Modifier.size(28.dp),
					tint = MaterialTheme.colorScheme.onPrimaryContainer
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					stringResource(R.string.informativa),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onPrimaryContainer,
					fontWeight = FontWeight.SemiBold
				)
			}
			Text(
				stringResource(R.string.informativa_description),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
				lineHeight = 20.sp
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(
					onClick = onShowClick,
					colors = ButtonDefaults.textButtonColors(
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
					)
				) {
					Text(stringResource(R.string.read))
				}
				Spacer(modifier = Modifier.width(12.dp))
				Button(
					onClick = onAcceptClick,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
						contentColor = MaterialTheme.colorScheme.primaryContainer
					)
				) {
					Text(stringResource(R.string.understood))
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
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 4.dp
		)
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					Icons.Rounded.ExtensionOff,
					contentDescription = null,
					modifier = Modifier.size(28.dp),
					tint = MaterialTheme.colorScheme.onSecondaryContainer
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					stringResource(R.string.no_active_plugins),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onSecondaryContainer,
					fontWeight = FontWeight.SemiBold
				)
			}
			Text(
				stringResource(R.string.no_plugins_description),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
				lineHeight = 20.sp
			)
			Button(
				onClick = onGetStartedClick,
				modifier = Modifier.align(Alignment.End),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
					contentColor = MaterialTheme.colorScheme.secondaryContainer
				)
			) {
				Text(stringResource(R.string.get_started))
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
		),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 4.dp,
			pressedElevation = 8.dp
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(20.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			val composition by rememberLottieComposition(
				spec = LottieCompositionSpec.RawRes(if (isAccessibilityGranted) R.raw.service_enabled else R.raw.service_disabled)
			)
			LottieAnimation(
				composition = composition,
				iterations = LottieConstants.IterateForever,
				contentScale = ContentScale.Fit,
				modifier = Modifier.size(72.dp)
			)

			Spacer(modifier = Modifier.width(20.dp))

			Column(
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = if (isAccessibilityGranted) stringResource(R.string.service_active) else stringResource(R.string.service_disabled),
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
					letterSpacing = 1.sp,
					color = if (isAccessibilityGranted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
				)
				Text(
					text = if (isAccessibilityGranted) stringResource(R.string.tap_to_disable) else stringResource(R.string.tap_to_enable),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@SuppressLint("UseKtx", "BatteryLife")
@Composable
fun OptimizationCard(
	startForResult: ManagedActivityResultLauncher<Intent, ActivityResult>,
	onDismiss: () -> Unit
) {
	val context = LocalContext.current
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 4.dp
		)
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					Icons.Rounded.Warning,
					contentDescription = null,
					modifier = Modifier.size(28.dp),
					tint = MaterialTheme.colorScheme.onTertiaryContainer
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					stringResource(R.string.battery_optimization_title),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onTertiaryContainer,
					fontWeight = FontWeight.SemiBold
				)
			}
			Text(
				stringResource(R.string.battery_optimization_description),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
				lineHeight = 20.sp
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(
					onClick = onDismiss,
					colors = ButtonDefaults.textButtonColors(
						contentColor = MaterialTheme.colorScheme.onTertiaryContainer
					)
				) {
					Text(stringResource(R.string.ignore))
				}
				Spacer(modifier = Modifier.width(12.dp))
				Button(
					onClick = {
						startForResult.launch(
							Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
								data = "package:${context.packageName}".toUri()
							}
						)
					},
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
						contentColor = MaterialTheme.colorScheme.tertiaryContainer
					)
				) {
					Text(stringResource(R.string.disable_optimization))
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
			.padding(vertical = 12.dp, horizontal = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(28.dp)
		)
		Spacer(modifier = Modifier.width(16.dp))
		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				fontWeight = FontWeight.Medium
			)
			Text(
				text = description,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				lineHeight = 18.sp
			)
		}
		Switch(
			checked = checked,
			onCheckedChange = { onClick() },
			colors = SwitchDefaults.colors(
				checkedThumbColor = MaterialTheme.colorScheme.primary,
				checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
				uncheckedThumbColor = MaterialTheme.colorScheme.outline,
				uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
			)
		)
	}
}