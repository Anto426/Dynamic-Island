package com.anto426.dynamicisland.ui.settings.pages

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.rememberDrawablePainter
import com.anto426.dynamicisland.MainActivity
import com.anto426.dynamicisland.island.IslandSettings
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Schermata per la selezione delle app abilitate, riprogettata con un design a schede.
 */
@Composable
fun EnabledAppsSettingsScreen() {
	val context = LocalContext.current
	val apps = remember { mutableStateListOf<PackageInfo>() }
	val executor: ExecutorService = Executors.newSingleThreadExecutor()

	// Carica le app in background per non bloccare l'UI
	LaunchedEffect(Unit) {
		executor.execute {
			apps.addAll(
				context.packageManager.getInstalledPackages(0)
					.filter { it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) == 0 }
					.toMutableList()
					.sortedBy { it.applicationInfo?.loadLabel(context.packageManager).toString().lowercase() }
			)
		}
	}

	// Aggiunge un'icona alla barra delle azioni per selezionare/deselezionare tutte le app
	LaunchedEffect(apps.size, IslandSettings.instance.enabledApps.size) {
		// Questa parte del codice dipende da una logica esterna, che non abbiamo modificato
	}

	// LazyColumn principale per l'intera schermata
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// La Card che contiene il titolo e l'intera lista delle app
		item {
			Card(
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "App Abilitate",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					SettingsDivider()
					// Ora la lista delle app è contenuta all'interno della stessa Card del titolo
					apps.forEach { app ->
						AppSettingItem(
							app = app,
							isEnabled = IslandSettings.instance.enabledApps.contains(app.packageName),
							onCheckedChange = { isChecked ->
								if (isChecked) {
									IslandSettings.instance.enabledApps.add(app.packageName)
								} else {
									IslandSettings.instance.enabledApps.remove(app.packageName)
								}
								IslandSettings.instance.applySettings(context)
							}
						)
					}
				}
			}
		}
	}
}

/**
 * Componente riutilizzabile per una singola app con un'opzione di switch.
 */
@Composable
fun AppSettingItem(
	app: PackageInfo,
	isEnabled: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onCheckedChange(!isEnabled) }
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Image(
			painter = rememberDrawablePainter(app.applicationInfo?.loadIcon(LocalContext.current.packageManager)),
			contentDescription = "Icona dell'app: ${app.applicationInfo?.loadLabel(LocalContext.current.packageManager).toString()}",
			modifier = Modifier
				.size(40.dp)
				.clip(CircleShape)
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = app.applicationInfo?.loadLabel(LocalContext.current.packageManager).toString(),
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.bodyLarge,
		)
		Spacer(modifier = Modifier.width(16.dp))
		Switch(
			checked = isEnabled,
			onCheckedChange = onCheckedChange
		)
	}
}

