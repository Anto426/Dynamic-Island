package com.anto426.dynamicisland.ui.settings.pages

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.STYLE
import com.anto426.dynamicisland.model.THEME
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.ui.settings.SettingsDivider
import com.anto426.dynamicisland.ui.settings.radioOptions
import com.anto426.dynamicisland.ui.theme.Theme
import androidx.core.content.edit

/**
 * Schermata delle impostazioni del tema con un design pulito e a schede.
 */
@Composable
fun ThemeSettingsScreen() {

	val context = LocalContext.current

	val isSystemInDarkTheme = isSystemInDarkTheme()

	// Shared Preferences
	val settingsPreferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)

	val (themeSelectedOption, onThemeOptionSelected) = remember { mutableStateOf(settingsPreferences.getString(THEME, "System")) }
	val (styleSelectedOption, onStyleOptionSelected) = remember { mutableStateOf(Theme.instance.themeStyle) }

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// Sezione per le impostazioni generali dell'isola
		item {
			SettingSwitch(
				title = "Mostra bordi",
				description = "Mostra i bordi attorno all'isola",
				checked = IslandSettings.instance.showBorders
			) {
				IslandSettings.instance.showBorders = it
				IslandSettings.instance.applySettings(context)
			}
		}

		// Sezione principale delle impostazioni di tema e stile
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
			) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					// Sottosezione: Preferenza del Tema
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							text = "Preferenza del Tema",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.fillMaxWidth()
						)
						Column(Modifier.selectableGroup()) {
							radioOptions.forEach { text ->
								ThemeRadioButton(
									text = text,
									selected = (text == themeSelectedOption)
								) {
									onThemeOptionSelected(text)
									settingsPreferences
										.edit {
                                            putString(THEME, text)
                                        }
									Theme.instance.isDarkTheme = when (text) {
										"System" -> isSystemInDarkTheme
										"Dark" -> true
										"Light" -> false
										else -> isSystemInDarkTheme
									}
								}
							}
						}
					}

					SettingsDivider()

					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							text = "Preferenza dello Stile",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.fillMaxWidth()
						)
						Column(Modifier.selectableGroup()) {
							Theme.ThemeStyle.entries.forEach { themeStyle ->
								StyleRadioButton(
									themeStyle = themeStyle,
									selected = (themeStyle == styleSelectedOption)
								) {
									onStyleOptionSelected(themeStyle)
									Theme.instance.themeStyle = themeStyle
									settingsPreferences
										.edit {
                                            putString(STYLE, themeStyle.name)
                                        }
								}
							}
						}
					}
				}
			}
		}
	}
}

/**
 * Componente riutilizzabile per una singola opzione di tema.
 */
@Composable
fun ThemeRadioButton(
	text: String,
	selected: Boolean,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp)
			.clip(MaterialTheme.shapes.medium)
			.selectable(
				selected = selected,
				onClick = onClick,
				role = Role.RadioButton
			)
			.padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		RadioButton(selected = selected, onClick = null)
		Text(
			text = text,
			style = MaterialTheme.typography.bodyLarge,
			modifier = Modifier.padding(start = 16.dp)
		)
	}
}

/**
 * Componente riutilizzabile per una singola opzione di stile.
 */
@Composable
fun StyleRadioButton(
	themeStyle: Theme.ThemeStyle,
	selected: Boolean,
	onClick: () -> Unit
) {
	val context = LocalContext.current
	val isDarkTheme = isSystemInDarkTheme()

	val stylePreviewColor = remember(themeStyle, isDarkTheme) {
		if (themeStyle.name == Theme.ThemeStyle.MaterialYou.name) {
			if (isDarkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
		} else {
			if (isDarkTheme) themeStyle.previewColorDark else themeStyle.previewColorLight
		}
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp)
			.clip(MaterialTheme.shapes.medium)
			.selectable(
				selected = selected,
				onClick = onClick,
				role = Role.RadioButton
			)
			.padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		RadioButton(
			selected = selected,
			onClick = null,
			colors = RadioButtonDefaults.colors(
				selectedColor = stylePreviewColor ?: MaterialTheme.colorScheme.primary,
				unselectedColor = stylePreviewColor ?: MaterialTheme.colorScheme.onSurfaceVariant
			)
		)
		Text(
			text = themeStyle.styleName,
			style = MaterialTheme.typography.bodyLarge,
			modifier = Modifier.padding(start = 16.dp).weight(1f)
		)
		Box(
			modifier = Modifier
				.size(24.dp)
				.aspectRatio(1f)
				.background(
					color = stylePreviewColor ?: MaterialTheme.colorScheme.primary,
					shape = CircleShape
				)
		)
	}
}


/**
 * Un elemento delle impostazioni con uno switch.
 */
@Composable
fun SettingSwitch(
	title: String,
	description: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onCheckedChange(!checked) }
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(text = title, style = MaterialTheme.typography.titleMedium)
			Text(
				text = description,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Spacer(Modifier.width(16.dp))
		Switch(checked = checked, onCheckedChange = onCheckedChange)
	}
}