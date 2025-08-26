package com.anto426.dynamicisland.ui.settings.pages

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.model.GRAVITY
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.island.IslandGravity
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.island.IslandViewState
import java.math.RoundingMode
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberUpdatedState

/**
 * Schermata per la configurazione della posizione e delle dimensioni dell'isola.
 * Utilizza un design pulito e organizzato in schede.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionSizeSettingsScreen() {
	val context = LocalContext.current
	val settingsPreferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)

	var expanded by remember { mutableStateOf(false) }
	var selectedGravity by rememberSaveable {
		mutableStateOf(IslandGravity.valueOf(settingsPreferences.getString(GRAVITY, IslandGravity.Center.name) ?: IslandGravity.Center.name))
	}

	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// Sezione: Posizione
		item {
			SettingsCard(title = "Posizione") {
				Box(
					modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)
				) {
					OutlinedTextField(
						value = selectedGravity.name,
						onValueChange = {},
						label = { Text("Gravità Isola") },
						modifier = Modifier.fillMaxWidth(),
						readOnly = true,
						trailingIcon = {
							IconButton(onClick = { expanded = !expanded }) {
								Icon(
									imageVector = Icons.Filled.ArrowDropDown,
									contentDescription = "Espandi"
								)
							}
						}
					)
					DropdownMenu(
						expanded = expanded,
						onDismissRequest = { expanded = false }
					) {
						IslandGravity.values().forEach { islandGravity ->
							DropdownMenuItem(
								text = { Text(text = islandGravity.name) },
								onClick = {
									selectedGravity = islandGravity
									expanded = false
									settingsPreferences.edit().putString(GRAVITY, islandGravity.name).apply()
									IslandSettings.instance.gravity = islandGravity
								}
							)
						}
					}
				}
				SettingsSliderItem(
					title = "Posizione X",
					extension = "dp",
					value = IslandSettings.instance.positionX.toFloat(),
					range = -LocalConfiguration.current.screenWidthDp.toFloat() / 2..LocalConfiguration.current.screenWidthDp.toFloat() / 2,
					onValueChange = {
						IslandSettings.instance.positionX = it.roundToInt()
						IslandSettings.instance.applySettings(context)
					},
					onReset = {
						IslandSettings.instance.positionX = 0
						IslandSettings.instance.applySettings(context)
					}
				)
				SettingsSliderItem(
					title = "Posizione Y",
					extension = "dp",
					value = IslandSettings.instance.positionY.toFloat(),
					range = 0f..50f,
					onValueChange = {
						IslandSettings.instance.positionY = it.roundToInt()
						IslandSettings.instance.applySettings(context)
					},
					onReset = {
						IslandSettings.instance.positionY = 5
						IslandSettings.instance.applySettings(context)
					}
				)
			}
		}

		// Sezione: Dimensione
		item {
			SettingsCard(title = "Dimensione") {
				SettingsSliderItem(
					title = "Larghezza",
					extension = "dp",
					value = IslandSettings.instance.width.toFloat(),
					range = IslandViewState.Opened.height.value * 3..LocalConfiguration.current.screenWidthDp.toFloat() - IslandViewState.Opened.yPosition.value * 2,
					onValueChange = {
						IslandSettings.instance.width = it.roundToInt()
						IslandSettings.instance.applySettings(context)
					},
					onReset = {
						IslandSettings.instance.width = 150
						IslandSettings.instance.applySettings(context)
					}
				)
				SettingsSliderItem(
					title = "Altezza",
					extension = "dp",
					value = IslandSettings.instance.height.toFloat(),
					range = 1f..LocalConfiguration.current.screenHeightDp.toFloat() / 2,
					onValueChange = {
						IslandSettings.instance.height = it.roundToInt()
						IslandSettings.instance.applySettings(context)
					},
					onReset = {
						IslandSettings.instance.height = 200
						IslandSettings.instance.applySettings(context)
					}
				)
			}
		}

		// Sezione: Angoli
		item {
			SettingsCard(title = "Angoli") {
				SettingsSliderItem(
					title = "Raggio Angoli",
					extension = "dp",
					value = IslandSettings.instance.cornerRadius.toFloat(),
					range = 0f..100f,
					onValueChange = {
						IslandSettings.instance.cornerRadius = it.roundToInt()
						IslandSettings.instance.applySettings(context)
					},
					onReset = {
						IslandSettings.instance.cornerRadius = 60
						IslandSettings.instance.applySettings(context)
					}
				)
			}
		}
	}
}

/**
 * Scheda riutilizzabile per raggruppare le impostazioni.
 */
@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
			Spacer(modifier = Modifier.height(16.dp))
			content()
		}
	}
}

/**
 * Elemento riutilizzabile per un cursore delle impostazioni con titolo e valore.
 */
@Composable
fun SettingsSliderItem(
	title: String,
	extension: String,
	value: Float,
	range: ClosedFloatingPointRange<Float>,
	onValueChange: (Float) -> Unit,
	onReset: () -> Unit,
	preciseValue: Float = 1f
) {
	val sliderValue by rememberUpdatedState(value)

	Column(
		modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleSmall
			)
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = "${sliderValue.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toFloat()}$extension",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.primary
				)
				TextButton(onClick = onReset, modifier = Modifier.padding(start = 8.dp)) {
					Text("Reset")
				}
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(
				onClick = { onValueChange(sliderValue - preciseValue) },
				enabled = sliderValue > range.start
			) {
				Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Decrease")
			}
			Slider(
				modifier = Modifier.weight(1f),
				value = sliderValue,
				onValueChange = onValueChange,
				valueRange = range
			)
			IconButton(
				onClick = { onValueChange(sliderValue + preciseValue) },
				enabled = sliderValue < range.endInclusive
			) {
				Icon(Icons.Default.ArrowRight, contentDescription = "Increase")
			}
		}
	}
}