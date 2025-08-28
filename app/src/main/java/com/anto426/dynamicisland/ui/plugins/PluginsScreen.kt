package com.anto426.dynamicisland.ui.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.R

/**
 * Main screen to manage plugins, with a modern card-based design and animations.
 */
@Composable
fun PluginScreen(
	onPluginClicked: (BasePlugin) -> Unit,
) {
	val enabledPluginsCount = ExportedPlugins.plugins.count { it.enabled.value }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		// Header informativo centrato
		PluginHeaderCard(enabledPluginsCount = enabledPluginsCount)

		// Lista dei plugin con padding ottimale
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			items(ExportedPlugins.plugins) { plugin ->
				PluginCard(
					plugin = plugin,
					onPluginClicked = onPluginClicked
				)
			}

			// Spazio finale
			item {
				Spacer(modifier = Modifier.height(20.dp))
			}
		}
	}
}

/**
 * Header card with plugin statistics and information - ottimizzato per centratura.
 */
@Composable
private fun PluginHeaderCard(enabledPluginsCount: Int) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 20.dp, vertical = 16.dp),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 4.dp
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			// Icona principale centrata
			Box(
				modifier = Modifier
					.size(64.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = Icons.Rounded.Extension,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
					modifier = Modifier.size(32.dp)
				)
			}

			// Titolo centrato
			Text(
				text = stringResource(id = R.string.plugins_enabled_title),
				style = MaterialTheme.typography.headlineSmall,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				fontWeight = FontWeight.Bold,
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)

			// Statistiche centrate
			Text(
				text = "$enabledPluginsCount di ${ExportedPlugins.plugins.size} plugin attivi",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

/**
 * Enhanced plugin card with modern design - semplificato senza espansione.
 */
@Composable
private fun PluginCard(
	plugin: BasePlugin,
	onPluginClicked: (BasePlugin) -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp)),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		),
		elevation = CardDefaults.cardElevation(
			defaultElevation = 0.dp
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onPluginClicked(plugin) }
				.padding(20.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			// Status indicator senza distinzione di colore
			Box(
				modifier = Modifier
					.size(48.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.surfaceVariant),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = if (plugin.enabled.value) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
					contentDescription = if (plugin.enabled.value)
						stringResource(id = R.string.plugin_status_enabled)
					else
						stringResource(id = R.string.plugin_status_disabled),
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.size(24.dp)
				)
			}

			// Informazioni plugin essenziali
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				Text(
					text = plugin.name,
					style = MaterialTheme.typography.titleLarge,
					color = MaterialTheme.colorScheme.onSurface,
					fontWeight = FontWeight.SemiBold,
					maxLines = 1
				)
				Text(
					text = plugin.description,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					lineHeight = 18.sp,
					maxLines = 2
				)
			}

			// Freccia per indicare navigazione
			Icon(
				imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
				contentDescription = "Vai ai dettagli",
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.size(24.dp)
			)
		}
	}
}