package com.anto426.dynamicisland.ui.plugins

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.ui.settings.pages.SettingsDivider

/**
 * Main screen to manage plugins, with a clean card-based design.
 */
@Composable
fun PluginScreen(
	onPluginClicked: (BasePlugin) -> Unit,
) {
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		contentPadding = PaddingValues(16.dp)
	) {
		item {
			Card(modifier = Modifier.fillMaxWidth()) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Plugin Abilitati",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					SettingsDivider()

					ExportedPlugins.plugins.forEach { plugin ->
						PluginItem(
							plugin = plugin,
							onPluginClicked = onPluginClicked
						)
					}
				}
			}
		}
	}
}

/**
 * A reusable item for a single plugin in the list.
 */
@Composable
fun PluginItem(
	plugin: BasePlugin,
	onPluginClicked: (BasePlugin) -> Unit
) {
	val context = LocalContext.current

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.clickable { onPluginClicked(plugin) }
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		// Plugin status indicator
		Icon(
			imageVector = if (plugin.enabled.value) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
			contentDescription = if (plugin.enabled.value) "Plugin abilitato" else "Plugin disabilitato",
			tint = if (plugin.enabled.value) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(32.dp)
		)
		Spacer(modifier = Modifier.width(16.dp))

		// Plugin name and description
		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(
				text = plugin.name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = plugin.description,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		Spacer(modifier = Modifier.width(16.dp))

		// Switch to enable/disable plugin
		Switch(
			checked = plugin.enabled.value && plugin.allPermissionsGranted,
			onCheckedChange = {
				plugin.switchEnabled(context, it)
			},
			enabled = plugin.allPermissionsGranted
		)
	}
}