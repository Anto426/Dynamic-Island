package com.anto426.dynamicisland.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Main settings screen that displays a list of setting items.
 */
@Composable
fun SettingsScreen(
	onSettingClicked: (SettingItem) -> Unit,
) {
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		item {
			// Group the settings into a single card for a cleaner look
			SettingsCard {
				settings.forEachIndexed { index, setting ->
					val settingsItem = setting as SettingItem
					SettingsItem(
						title = settingsItem.title,
						subtitle = settingsItem.subtitle,
						icon = settingsItem.icon,
						onClick = { onSettingClicked(settingsItem) }
					)
					// Add a divider between items, but not after the last one
					if (index < settings.size - 1) {
						SettingsDivider()
					}
				}
			}
		}
	}
}

/**
 * A single item in the settings list.
 */
@Composable
fun SettingsItem(
	title: String,
	subtitle: String,
	icon: ImageVector,
	onClick: () -> Unit
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val animatedBackgroundColor by animateColorAsState(
		targetValue = if (isPressed) {
			MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
		} else {
			Color.Transparent
		},
		label = "clickAnimation"
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.background(animatedBackgroundColor)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				onClick = onClick
			)
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(40.dp)
				.clip(CircleShape)
				.background(MaterialTheme.colorScheme.primaryContainer),
			contentAlignment = Alignment.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onPrimaryContainer
			)
		}
		Spacer(modifier = Modifier.width(16.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
			if (subtitle.isNotEmpty()) {
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

/**
 * Reusable card to group settings.
 */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
		) {
			content()
		}
	}
}

/**
 * Reusable divider for separating settings items.
 */
@Composable
fun SettingsDivider() {
	Divider(
		modifier = Modifier.padding(horizontal = 8.dp),
		color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
	)
}