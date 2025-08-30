package com.anto426.dynamicisland.ui.settings.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.ui.settings.pages.EnhancedSettingSwitch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Vibration

/**
 * Schermata completamente ridisegnata per le impostazioni di comportamento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorSettingsScreen() {
    val context = LocalContext.current


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Header informativo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsSuggest,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_behavior_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(id = R.string.settings_behavior_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Sezione: Visibilità
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.behavior_visibility_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EnhancedSettingSwitch(
                                title = stringResource(id = R.string.behavior_show_on_lockscreen_title),
                                description = stringResource(id = R.string.behavior_show_on_lockscreen_description),
                                icon = Icons.Default.ScreenLockPortrait,
                                checked = IslandSettings.instance.showOnLockScreen,
                                onCheckedChange = {
                                    IslandSettings.instance.showOnLockScreen = it
                                    IslandSettings.instance.applySettings(context)
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            EnhancedSettingSwitch(
                                title = stringResource(id = R.string.behavior_show_in_landscape_title),
                                description = stringResource(id = R.string.behavior_show_in_landscape_description),
                                icon = Icons.Default.ScreenRotation,
                                checked = IslandSettings.instance.showInLandscape,
                                onCheckedChange = {
                                    IslandSettings.instance.showInLandscape = it
                                    IslandSettings.instance.applySettings(context)
                                }
                            )
                        }
                    }
                }
            }

            // Sezione: Auto-nascondimento
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.behavior_autohide_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.behavior_autohide_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            EnhancedSliderItem(
                                title = stringResource(id = R.string.behavior_autohide_opened_island_title),
                                value = IslandSettings.instance.autoHideOpenedAfter / 1000f,
                                range = 0.5f..60f,
                                onValueChange = {
                                    IslandSettings.instance.autoHideOpenedAfter = it * 1000
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.Schedule,
                                unit = "s"
                            )
                        }
                    }
                }
            }

            // Sezione: Animazioni e feedback (spostata da Avanzate)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.behavior_animations_feedback_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EnhancedSettingSwitch(
                                title = stringResource(id = R.string.advanced_settings_enable_animations),
                                description = stringResource(id = R.string.advanced_settings_enable_animations_desc),
                                icon = Icons.Default.Animation,
                                checked = IslandSettings.instance.animationsEnabled,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.animationsEnabled = enabled
                                    IslandSettings.instance.applySettings(context)
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            EnhancedSettingSwitch(
                                title = stringResource(id = R.string.advanced_settings_haptic),
                                description = stringResource(id = R.string.advanced_settings_haptic_desc),
                                icon = Icons.Default.Vibration,
                                checked = IslandSettings.instance.hapticFeedback,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.hapticFeedback = enabled
                                    IslandSettings.instance.applySettings(context)
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            EnhancedSettingSwitch(
                                title = stringResource(id = R.string.advanced_settings_sounds),
                                description = stringResource(id = R.string.advanced_settings_sounds_desc),
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                checked = IslandSettings.instance.soundEnabled,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.soundEnabled = enabled
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
 * Componente avanzato per switch delle impostazioni con icona e design moderno
 */


/**
 * Componente avanzato per slider delle impostazioni con icona
 */
@Composable
fun EnhancedSliderItem(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    unit: String = ""
) {
    var sliderValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header con icona, titolo e valore
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${"%.1f".format(sliderValue)} $unit",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Slider
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}
