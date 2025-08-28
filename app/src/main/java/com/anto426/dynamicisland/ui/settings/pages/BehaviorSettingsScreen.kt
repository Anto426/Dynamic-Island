package com.anto426.dynamicisland.ui.settings.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.island.IslandSettings

/**
 * Redesigned Behavior Settings Screen with a clean, card-based layout.
 */
@Composable
fun BehaviorSettingsScreen() {
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// Main Card for the behavior settings
		item {
			Card(
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Visual Behavior",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Spacer(modifier = Modifier.height(8.dp))

					SettingSwitch(
						title = "Show on lock screen",
						description = "Show the island on the lock screen and on the always-on display",
						checked = IslandSettings.instance.showOnLockScreen
					) { IslandSettings.instance.showOnLockScreen = it }

					SettingsDivider()

					SettingSwitch(
						title = "Show in landscape",
						description = "Show island in landscape mode",
						checked = IslandSettings.instance.showInLandscape
					) { IslandSettings.instance.showInLandscape = it }
				}
			}
		}

		// Card for the auto-hide settings
		item {
			Card(
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Auto-hide",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Spacer(modifier = Modifier.height(8.dp))

					SettingsSliderItem(
						title = "Auto hide opened island after",
						extension = "s",
						value = IslandSettings.instance.autoHideOpenedAfter / 1000,
						range = 0.5f..60f,
						onValueChange = {
							IslandSettings.instance.autoHideOpenedAfter = it * 1000
						}
					)
				}
			}
		}
	}
}

/**
 * A reusable settings item with a slider.
 */
@Composable
fun SettingsSliderItem(
	title: String,
	extension: String,
	value: Float,
	range: ClosedFloatingPointRange<Float>,
	onValueChange: (Float) -> Unit
) {
	var sliderValue by remember { mutableFloatStateOf(value) }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 8.dp),
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(text = title, style = MaterialTheme.typography.titleMedium)
			Text(
				text = "${"%.1f".format(sliderValue)}$extension",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Spacer(Modifier.height(8.dp))
		Slider(
			value = sliderValue,
			onValueChange = {
				sliderValue = it
				onValueChange(it)
			},
			valueRange = range
		)
	}
}

/**
 * A simple divider to separate settings items.
 */
@Composable
fun SettingsDivider() {
	Divider(
		modifier = Modifier.padding(vertical = 8.dp),
		color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
	)
}