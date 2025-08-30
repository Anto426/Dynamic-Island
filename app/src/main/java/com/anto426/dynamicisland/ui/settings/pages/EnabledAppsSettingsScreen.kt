

package com.anto426.dynamicisland.ui.settings.pages

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.skydoves.landscapist.rememberDrawablePainter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Schermata completamente ridisegnata per la selezione delle app abilitate
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnabledAppsSettingsScreen() {
    val context = LocalContext.current
    val apps = remember { mutableStateListOf<PackageInfo>() }
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    var searchQuery by remember { mutableStateOf("") }

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

    // Filtra le app in base alla ricerca
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                val appName = app.applicationInfo?.loadLabel(context.packageManager).toString().lowercase()
                appName.contains(searchQuery.lowercase())
            }
        }
    }


        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header informativo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.enabled_apps_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(id = R.string.enabled_apps_summary, IslandSettings.instance.enabledApps.size, apps.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(id = R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.search_settings_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )

                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(id = R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Lista delle app
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    EnhancedAppSettingItem(
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

                // Messaggio quando non ci sono risultati
                if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = stringResource(id = R.string.no_results_found),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = stringResource(id = R.string.try_different_keywords),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
    }
}

/**
 * Componente avanzato per una singola app con design moderno
 */
@Composable
fun EnhancedAppSettingItem(
    app: PackageInfo,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "background"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(!isEnabled) }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icona dell'app
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Image(
                    painter = rememberDrawablePainter(app.applicationInfo?.loadIcon(context.packageManager)),
                    contentDescription = stringResource(id = R.string.app_icon_content_description, app.applicationInfo?.loadLabel(context.packageManager).toString()),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            // Informazioni dell'app
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.applicationInfo?.loadLabel(context.packageManager).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

