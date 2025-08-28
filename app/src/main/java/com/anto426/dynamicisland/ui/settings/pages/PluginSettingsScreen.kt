package com.anto426.dynamicisland.plugins

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.ui.settings.pages.SettingSwitch

// --- Nota: Le assunzioni sulla classe BasePlugin rimangono invariate ---
/*
data class BasePlugin(
    // ... campi esistenti
    ...
)
*/


@Composable
fun PluginSettingsScreen(
	plugin: BasePlugin
) {
	val context = LocalContext.current
	val uriHandler = LocalUriHandler.current

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// Card di intestazione con informazioni sul plugin
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(24.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Icon(
						imageVector = Icons.Default.Extension,
						contentDescription = "Icona del Plugin",
						modifier = Modifier.size(40.dp),
						tint = MaterialTheme.colorScheme.primary
					)
					Spacer(Modifier.height(12.dp))
					Text(plugin.name, style = MaterialTheme.typography.titleLarge)
					Text(
						"di ${plugin.author}",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(Modifier.height(8.dp))
					Text(
						plugin.description,
						style = MaterialTheme.typography.bodyMedium,
						textAlign = TextAlign.Center,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}

		// Card per Abilitare/Disabilitare
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
			) {
				SettingSwitch(
					title = "Abilita Plugin",
					description = if (plugin.allPermissionsGranted) "Attiva o disattiva questo plugin" else "Concedi i permessi per abilitare",
					checked = plugin.enabled.value,
					onCheckedChange = { plugin.switchEnabled(context) },
				)
			}
		}

		// Card Informazioni Plugin
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
			) {
				Column(modifier = Modifier.padding(vertical = 8.dp)) {
					Text(
						text = "Informazioni Plugin",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
					)
					PluginInfoItem(
						icon = Icons.Default.Info,
						title = "Versione",
						value = plugin.version,
						isCopyable = true
					)
					SettingsDivider()
					PluginInfoItem(
						icon = Icons.Default.Fingerprint,
						title = "Identifier",
						value = plugin.id,
						isCopyable = true
					)
					plugin.sourceCodeUrl?.let { url ->
						SettingsDivider()
						PluginInfoItem(
							icon = Icons.Default.Code,
							title = "Codice Sorgente",
							value = "Visualizza su web",
							onClick = { uriHandler.openUri(url as String) }
						)
					}
				}
			}
		}

		// Card Permessi
		if (plugin.permissions.isNotEmpty()) {
			item {
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
				) {
					Column(modifier = Modifier.padding(vertical = 8.dp)) {
						Text(
							text = "Permessi richiesti",
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
						)
						plugin.permissions.forEachIndexed { index, permissionKey ->
							val permission = ExportedPlugins.permissions[permissionKey]!!
							// MODIFICA: Passiamo l'intero oggetto 'plugin'
							PermissionSettings(permission = permission, plugin = plugin)
							if (index < plugin.permissions.size - 1) {
								SettingsDivider()
							}
						}
					}
				}
			}
		}

		// Card Impostazioni
		if (plugin.pluginSettings.isNotEmpty()) {
			item {
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
				) {
					Column(modifier = Modifier.padding(vertical = 8.dp)) {
						Text(
							text = "Impostazioni Plugin",
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
						)
						val settingsList = plugin.pluginSettings.values.toList()
						settingsList.forEachIndexed { index, settings ->
							when (settings) {
								is PluginSettingsItem.SwitchSettingsItem -> SettingSwitch(
									title = settings.title,
									description = settings.description,
									checked = settings.value.value,
									onCheckedChange = { settings.onValueChange(context, it) }
								)
							}
							if (index < settingsList.size - 1) {
								SettingsDivider()
							}
						}
					}
				}
			}
		}
	}
}

@Composable
fun PermissionSettings(
	modifier: Modifier = Modifier,
	permission: PluginPermission,
	plugin: BasePlugin // MODIFICA: Riceve l'oggetto plugin
) {
	val context = LocalContext.current
	permission.granted.value = permission.checkPermission(context)

	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		val wasGrantedBefore = permission.granted.value
		val isGrantedNow = permission.checkPermission(context)
		permission.granted.value = isGrantedNow

		// MODIFICA: Controlla se il permesso è stato APPENA concesso
		if (!wasGrantedBefore && isGrantedNow) {
			// Controlla se ORA tutti i permessi sono stati concessi per questo plugin
			val allPermissionsAreNowGranted = plugin.permissions.all { permKey ->
				ExportedPlugins.permissions[permKey]?.checkPermission(context) == true
			}

			// Se tutti sono concessi e il plugin non è già attivo, mostra il messaggio
			if (allPermissionsAreNowGranted && !plugin.enabled.value) {
				Toast.makeText(
					context,
					"Permessi concessi! Ora puoi abilitare il plugin.",
					Toast.LENGTH_LONG
				).show()
			}
		}
	}

	Row(
		modifier = modifier
			.fillMaxWidth()
			.clickable { launcher.launch(permission.requestIntent) }
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = Icons.Filled.Shield,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSecondaryContainer,
			modifier = Modifier.size(24.dp)
		)
		Spacer(Modifier.width(16.dp))
		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(
				text = permission.name,
				style = MaterialTheme.typography.bodyLarge
			)
			Text(
				text = permission.description,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Spacer(Modifier.width(16.dp))
		Switch(
			checked = permission.granted.value,
			onCheckedChange = {
				launcher.launch(permission.requestIntent)
			}
		)
	}
}

@Composable
fun PluginInfoItem(
	icon: ImageVector,
	title: String,
	value: String,
	isCopyable: Boolean = false,
	onClick: (() -> Unit)? = null
) {
	val context = LocalContext.current
	val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
	val isClickable = isCopyable || onClick != null

	val onAction: () -> Unit = {
		when {
			onClick != null -> onClick.invoke()
			isCopyable && clipboardManager != null -> {
				clipboardManager.setPrimaryClip(ClipData.newPlainText(title, value))
				Toast.makeText(context, "$title copiato negli appunti!", Toast.LENGTH_SHORT).show()
			}
		}
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.clickable(enabled = isClickable, onClick = onAction)
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSecondaryContainer,
			modifier = Modifier.size(24.dp)
		)
		Spacer(Modifier.width(16.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyLarge
			)
			Text(
				text = value,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
fun SettingsDivider() {
	HorizontalDivider(
		modifier = Modifier.padding(horizontal = 16.dp),
		thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
	)
}