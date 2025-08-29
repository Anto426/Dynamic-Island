package com.anto426.dynamicisland.ui.settings.pages

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.model.SETTINGS_CHANGED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(24.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.advanced_settings_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.advanced_settings_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Animazioni
        item {
            SettingGroup(title = stringResource(R.string.advanced_settings_animations)) {
                EnhancedSettingSwitch(
                    title = stringResource(R.string.advanced_settings_enable_animations),
                    description = stringResource(R.string.advanced_settings_enable_animations_desc),
                    icon = Icons.Default.Animation,
                    checked = IslandSettings.instance.animationsEnabled,
                    onCheckedChange = { enabled ->
                        IslandSettings.instance.animationsEnabled = enabled
                        IslandSettings.instance.applySettings(context)
                        context.sendBroadcast(Intent(SETTINGS_CHANGED))
                    }
                )
            }
        }

        // Feedback
        item {
            SettingGroup(title = stringResource(R.string.advanced_settings_feedback)) {
                EnhancedSettingSwitch(
                    title = stringResource(R.string.advanced_settings_haptic),
                    description = stringResource(R.string.advanced_settings_haptic_desc),
                    icon = Icons.Default.Vibration,
                    checked = IslandSettings.instance.hapticFeedback,
                    onCheckedChange = { enabled ->
                        IslandSettings.instance.hapticFeedback = enabled
                        IslandSettings.instance.applySettings(context)
                        context.sendBroadcast(Intent(SETTINGS_CHANGED))
                    }
                )

                SettingsDivider()

                EnhancedSettingSwitch(
                    title = stringResource(R.string.advanced_settings_sounds),
                    description = stringResource(R.string.advanced_settings_sounds_desc),
                    icon = Icons.Default.VolumeUp,
                    checked = IslandSettings.instance.soundEnabled,
                    onCheckedChange = { enabled ->
                        IslandSettings.instance.soundEnabled = enabled
                        IslandSettings.instance.applySettings(context)
                        context.sendBroadcast(Intent(SETTINGS_CHANGED))
                    }
                )
            }
        }

        // Comportamento
        item {
            SettingGroup(title = stringResource(R.string.advanced_settings_behavior)) {
                Text(
                    text = stringResource(R.string.advanced_settings_behavior_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tema Dinamico
        item {
            SettingGroup(title = stringResource(R.string.advanced_settings_dynamic_theme)) {
                EnhancedSettingSwitch(
                    title = stringResource(R.string.advanced_settings_dynamic_theme_title),
                    description = stringResource(R.string.advanced_settings_dynamic_theme_desc),
                    icon = Icons.Default.BatteryFull,
                    checked = IslandSettings.instance.dynamicThemeEnabled,
                    onCheckedChange = { enabled ->
                        IslandSettings.instance.dynamicThemeEnabled = enabled
                        IslandSettings.instance.applySettings(context)
                        context.sendBroadcast(Intent(SETTINGS_CHANGED))
                    }
                )
            }
        }
    }
}
