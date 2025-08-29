package com.anto426.dynamicisland.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anto426.dynamicisland.R

@Composable
fun SettingsScreen(
    onSettingClicked: (SettingItem) -> Unit,
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val isSearching = searchQuery.text.isNotEmpty()

    // Filtra le impostazioni in base alla ricerca
    val filteredSettings = remember(searchQuery.text) {
        if (searchQuery.text.isEmpty()) {
            settings
        } else {
            val query = searchQuery.text.lowercase()
            settings.filter { setting ->
                when (setting) {
                    ThemeSetting -> "theme|tema|dark|light|sistema".contains(query)
                    PositionSizeSetting -> "position|posizione|size|dimensione".contains(query)
                    BehaviorSetting -> "behavior|comportamento".contains(query)
                    EnabledAppsSetting -> "enabled|abilitate|apps|applicazioni".contains(query)
                    AboutSetting -> "about|info|informazioni".contains(query)
                    DeveloperScreen -> "dev|developer|sviluppatore".contains(query)
                    else -> false
                }
            }
        }
    }

    val groupedSettings = remember(filteredSettings) {
        if (isSearching) {
            mapOf("Risultati ricerca" to filteredSettings)
        } else {
            filteredSettings.groupBy { setting ->
                when (setting) {
                    ThemeSetting, PositionSizeSetting -> "Aspetto"
                    BehaviorSetting, EnabledAppsSetting -> "Comportamento"
                    AboutSetting, DeveloperScreen -> "Informazioni"
                    else -> "Altro"
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        contentDescription = "Cerca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.text.isEmpty()) {
                                Text(
                                    text = "Cerca impostazioni...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { searchQuery = TextFieldValue("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Cancella ricerca",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    top = 8.dp,
                    end = 24.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedSettings.forEach { (sectionName, sectionSettings) ->
                    // Header della sezione
                    item {
                        if (!isSearching) {
                            SettingsSectionHeader(sectionName)
                        }
                    }

                    // Elementi della sezione
                    items(sectionSettings, key = { setting -> (setting as SettingItem).route }) { setting ->
                        val settingsItem = setting as SettingItem
                        val titleResId = getStringResourceId(settingsItem.title)
                        val subtitleResId = getStringResourceId(settingsItem.subtitle)

                        EnhancedSettingsItem(
                            title = stringResource(id = titleResId),
                            subtitle = stringResource(id = subtitleResId),
                            icon = settingsItem.icon,
                            onClick = { onSettingClicked(settingsItem) }
                        )
                    }

                    // Spazio tra sezioni
                    item {
                        if (!isSearching) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Messaggio quando non ci sono risultati
                if (filteredSettings.isEmpty() && isSearching) {
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
                                text = "Nessun risultato trovato",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Prova con parole chiave diverse",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // FAB per azioni rapide
        if (!isSearching) {
            FloatingActionButton(
                onClick = { /* TODO: Implementare reset impostazioni */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset impostazioni"
                )
            }
        }
    }
}

@Composable
fun EnhancedSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "background"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with enhanced styling
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            // Arrow indicator
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Componente per l'header delle sezioni
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

// Helper function to get string resource ID from string name
@Composable
private fun getStringResourceId(stringName: String): Int {
    return when (stringName) {
        "settings_item_theme" -> R.string.settings_theme_title
        "settings_item_theme_subtitle" -> R.string.settings_theme_subtitle
        "settings_item_position_size" -> R.string.settings_position_size_title
        "settings_item_position_size_subtitle" -> R.string.settings_position_size_subtitle
        "settings_item_enabled_apps" -> R.string.enabled_apps_title
        "settings_item_enabled_apps_subtitle" -> R.string.settings_enabled_apps_subtitle
        "settings_item_behavior" -> R.string.settings_behavior_title
        "settings_item_behavior_subtitle" -> R.string.settings_behavior_subtitle
        "settings_item_about" -> R.string.settings_about_title
        "settings_item_about_subtitle" -> R.string.settings_about_subtitle
        "settings_item_dev" -> R.string.settings_dev_title
        "settings_item_dev_subtitle" -> R.string.settings_dev_subtitle
        "settings_item_updates" -> R.string.settings_item_updates
        "settings_item_updates_subtitle" -> R.string.settings_item_updates_subtitle
        else -> R.string.app_name
    }
}