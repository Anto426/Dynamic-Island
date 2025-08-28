package com.anto426.dynamicisland.ui.settings.pages

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.model.GRAVITY
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.island.IslandGravity
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.island.IslandViewState
import java.math.RoundingMode
import kotlin.math.roundToInt
import androidx.core.content.edit

/**
 * Schermata completamente ridisegnata per la configurazione della posizione e delle dimensioni dell'isola.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
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
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configura l'aspetto dell'isola",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Personalizza posizione, dimensioni e forma",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Sezione: Posizione
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Posizione",
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
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Dropdown per gravità
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(id = R.string.position_island_gravity),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedGravity.name,
                                        onValueChange = {},
                                        modifier = Modifier.fillMaxWidth(),
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = stringResource(id = R.string.position_expand_icon_desc)
                                                )
                                            }
                                        },
                                        shape = MaterialTheme.shapes.large,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        IslandGravity.entries.forEach { islandGravity ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = islandGravity.name,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                },
                                                onClick = {
                                                    selectedGravity = islandGravity
                                                    expanded = false
                                                    settingsPreferences.edit {
                                                        putString(GRAVITY, islandGravity.name)
                                                    }
                                                    IslandSettings.instance.gravity = islandGravity
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Slider per posizione X
                            EnhancedSliderItem(
                                title = stringResource(id = R.string.position_x_title),
                                value = IslandSettings.instance.positionX.toFloat(),
                                range = -LocalConfiguration.current.screenWidthDp.toFloat() / 2..LocalConfiguration.current.screenWidthDp.toFloat() / 2,
                                onValueChange = {
                                    IslandSettings.instance.positionX = it.roundToInt()
                                    IslandSettings.instance.applySettings(context)
                                },
                                onReset = {
                                    IslandSettings.instance.positionX = 0
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.HorizontalDistribute
                            )

                            // Slider per posizione Y
                            EnhancedSliderItem(
                                title = stringResource(id = R.string.position_y_title),
                                value = IslandSettings.instance.positionY.toFloat(),
                                range = 0f..50f,
                                onValueChange = {
                                    IslandSettings.instance.positionY = it.roundToInt()
                                    IslandSettings.instance.applySettings(context)
                                },
                                onReset = {
                                    IslandSettings.instance.positionY = 5
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.VerticalDistribute
                            )
                        }
                    }
                }
            }

            // Sezione: Dimensioni
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Dimensioni",
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
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Slider per larghezza
                            EnhancedSliderItem(
                                title = stringResource(id = R.string.size_width),
                                value = IslandSettings.instance.width.toFloat(),
                                range = IslandViewState.Opened.height.value * 3..LocalConfiguration.current.screenWidthDp.toFloat() - IslandViewState.Opened.yPosition.value * 2,
                                onValueChange = {
                                    IslandSettings.instance.width = it.roundToInt()
                                    IslandSettings.instance.applySettings(context)
                                },
                                onReset = {
                                    IslandSettings.instance.width = 150
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.WidthNormal
                            )

                            // Slider per altezza
                            EnhancedSliderItem(
                                title = stringResource(id = R.string.size_height),
                                value = IslandSettings.instance.height.toFloat(),
                                range = 1f..LocalConfiguration.current.screenHeightDp.toFloat() / 2,
                                onValueChange = {
                                    IslandSettings.instance.height = it.roundToInt()
                                    IslandSettings.instance.applySettings(context)
                                },
                                onReset = {
                                    IslandSettings.instance.height = 200
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.Height
                            )
                        }
                    }
                }
            }

            // Sezione: Angoli
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Stile",
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
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Slider per raggio angoli
                            EnhancedSliderItem(
                                title = stringResource(id = R.string.corners_radius),
                                value = IslandSettings.instance.cornerRadius.toFloat(),
                                range = 0f..100f,
                                onValueChange = {
                                    IslandSettings.instance.cornerRadius = it.roundToInt()
                                    IslandSettings.instance.applySettings(context)
                                },
                                onReset = {
                                    IslandSettings.instance.cornerRadius = 60
                                    IslandSettings.instance.applySettings(context)
                                },
                                icon = Icons.Default.RoundedCorner
                            )
                        }
                    }
                }
            }
        }
}

/**
 * Componente avanzato per slider delle impostazioni con icona e design moderno
 */
@Composable
fun EnhancedSliderItem(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    preciseValue: Float = 1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
        label = "background"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(backgroundColor, MaterialTheme.shapes.large)
            .padding(16.dp),
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
                    text = "${value.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toFloat()} dp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onReset,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.reset_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Controlli slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onValueChange(value - preciseValue) },
                enabled = value > range.start,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                    contentDescription = stringResource(id = R.string.decrease_icon_desc),
                    tint = if (value > range.start) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                modifier = Modifier.weight(1f),
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            IconButton(
                onClick = { onValueChange(value + preciseValue) },
                enabled = value < range.endInclusive,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = stringResource(id = R.string.increase_icon_desc),
                    tint = if (value < range.endInclusive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
