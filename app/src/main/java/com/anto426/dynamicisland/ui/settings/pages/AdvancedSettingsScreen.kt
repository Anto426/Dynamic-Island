package com.anto426.dynamicisland.ui.settings.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.anto426.dynamicisland.ui.settings.pages.EnhancedSettingSwitch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.updater.UpdateManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider

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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Impostazioni Avanzate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Configurazioni avanzate per personalizzare l'esperienza dell'isola dinamica",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Sezione Animazioni
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Animazioni",
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
                                title = "Abilita Animazioni",
                                description = "Attiva/disattiva tutte le animazioni dell'isola",
                                icon = Icons.Default.Animation,
                                checked = IslandSettings.instance.animationsEnabled,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.animationsEnabled = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )
                        }
                    }
                }
            }

            // Sezione Feedback
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Feedback",
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
                                title = "Haptic Feedback",
                                description = "Vibrazione al tocco",
                                icon = Icons.Default.Vibration,
                                checked = IslandSettings.instance.hapticFeedback,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.hapticFeedback = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            EnhancedSettingSwitch(
                                title = "Suoni di Sistema",
                                description = "Riproduci suoni per le interazioni",
                                icon = Icons.Default.VolumeUp,
                                checked = IslandSettings.instance.soundEnabled,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.soundEnabled = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )
                        }
                    }
                }
            }

            // Sezione Comportamento
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Comportamento",
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
                            // Auto-hide settings will be added here
                            Text(
                                text = "Impostazioni di temporizzazione verranno aggiunte qui",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Sezione Tema Dinamico
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Tema Dinamico",
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
                                title = "Tema Basato su Batteria",
                                description = "Cambia i colori in base al livello batteria",
                                icon = Icons.Default.BatteryFull,
                                checked = IslandSettings.instance.dynamicThemeEnabled,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.dynamicThemeEnabled = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )
                        }
                    }
                }
            }

            // Sezione Modalità Speciali
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Modalità Speciali",
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
                                title = "Modalità Silenziosa",
                                description = "Disabilita suoni e notifiche",
                                icon = Icons.Default.NotificationsOff,
                                checked = IslandSettings.instance.silentMode,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.silentMode = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            EnhancedSettingSwitch(
                                title = "Modalità Risparmio Energetico",
                                description = "Riduce animazioni e prestazioni per risparmiare batteria",
                                icon = Icons.Default.Power,
                                checked = IslandSettings.instance.lowPowerMode,
                                onCheckedChange = { enabled ->
                                    IslandSettings.instance.lowPowerMode = enabled
                                    IslandSettings.instance.applySettings(context)
                                    context.sendBroadcast(android.content.Intent(SETTINGS_CHANGED))
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            EnhancedSettingSwitch(
                                title = "Aggiornamenti Automatici",
                                description = "Controlla automaticamente nuovi aggiornamenti da GitHub",
                                icon = Icons.Default.Refresh,
                                checked = UpdateManager(context).isAutoUpdateEnabled(),
                                onCheckedChange = { enabled ->
                                    UpdateManager(context).setAutoUpdateEnabled(enabled)
                                }
                            )
                        }
                    }
                }
            }
        }
}
