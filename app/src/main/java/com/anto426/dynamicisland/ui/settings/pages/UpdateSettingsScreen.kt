package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.updater.LocalUpdateManager
import com.anto426.dynamicisland.updater.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSettingsScreen(viewModel: UpdateViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
    ) {

                item {
            AnimatedVisibility(
                    visible =
                            uiState.updateCheckState is
                                    UpdateViewModel.UpdateCheckState.UpdateAvailable ||
                                    uiState.updateCheckState is
                                            UpdateViewModel.UpdateCheckState.Idle ||
                                    uiState.updateCheckState is
                                            UpdateViewModel.UpdateCheckState.Checking ||
                                    uiState.updateCheckState is
                                            UpdateViewModel.UpdateCheckState.Error,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 })
            ) {
                if (uiState.updateCheckState is UpdateViewModel.UpdateCheckState.UpdateAvailable ||
                                uiState.updateCheckState is UpdateViewModel.UpdateCheckState.Idle ||
                                uiState.updateCheckState is
                                        UpdateViewModel.UpdateCheckState.Checking ||
                                uiState.updateCheckState is UpdateViewModel.UpdateCheckState.Error
                ) {
                    SettingGroup(stringResource(R.string.update_status_section)) {
                        UpdateStatusContent(uiState)
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                    visible =
                            uiState.updateCheckState is
                                    UpdateViewModel.UpdateCheckState.UpdateAvailable,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
            ) {
                if (uiState.updateCheckState is UpdateViewModel.UpdateCheckState.UpdateAvailable) {
                    SettingGroup(stringResource(R.string.changelog_section)) {
                        ChangelogContent(uiState)
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 })
            ) {
                SettingGroup(stringResource(R.string.manual_actions_section)) {
                    ManualActionsContent(uiState, viewModel)
                }
            }
        }

        item {
            AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 })
            ) {
                SettingGroup(stringResource(R.string.update_settings_section)) {
                    CompactSettingsContent(uiState, viewModel)
                }
            }
        }
    }
}

// ----------------- CONTENT COMPONENTS -----------------

@Composable
private fun UpdateStatusContent(uiState: UpdateViewModel.UiState) {
    val context = LocalContext.current

    // Status principale con animazione
    AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -100 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -100 })
    ) {
        when (val state = uiState.updateCheckState) {
            is UpdateViewModel.UpdateCheckState.Idle ->
                    EnhancedUpdateItem(
                            Icons.Default.CheckCircle,
                            stringResource(R.string.app_up_to_date),
                            stringResource(R.string.app_up_to_date_desc),
                            MaterialTheme.colorScheme.primary
                    )
            is UpdateViewModel.UpdateCheckState.Checking ->
                    EnhancedUpdateItem(
                            Icons.Default.Refresh,
                            stringResource(R.string.checking_updates),
                            stringResource(R.string.checking_updates_desc),
                            MaterialTheme.colorScheme.primary,
                            true
                    )
            is UpdateViewModel.UpdateCheckState.UpdateAvailable -> {
                val info = state.updateInfo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.update_available),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("NEW", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                stringResource(R.string.update_available_desc, info.latestVersion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            is UpdateViewModel.UpdateCheckState.Error ->
                    EnhancedUpdateItem(
                            Icons.Default.Error,
                            stringResource(R.string.update_error),
                            state.message,
                            MaterialTheme.colorScheme.error
                    )
        }
    }

    SettingsDivider()

    // Informazioni versione con copia al tap
    AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { 100 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { 100 })
    ) {
        EnhancedInfoItem(
                Icons.Default.Info,
                stringResource(R.string.current_version),
                uiState.currentVersion,
                isCopyable = true,
                context = context
        )
    }
}

@Composable
private fun CompactSettingsContent(uiState: UpdateViewModel.UiState, viewModel: UpdateViewModel) {
    val context = LocalContext.current
    var showAdvancedSettings by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Dropdown per il canale di rilascio
        ReleaseChannelDropdown(uiState, viewModel)

        // Pulsante per impostazioni avanzate
        OutlinedButton(
                onClick = { showAdvancedSettings = !showAdvancedSettings },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
        ) {
            Icon(
                    if (showAdvancedSettings) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                    contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                    if (showAdvancedSettings) stringResource(R.string.hide_advanced_settings)
                    else stringResource(R.string.show_advanced_settings)
            )
        }

        // Impostazioni avanzate espandibili
        AnimatedVisibility(
                visible = showAdvancedSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Auto-update
                EnhancedSettingSwitch(
                        stringResource(R.string.auto_update_title),
                        stringResource(R.string.auto_update_desc),
                        Icons.Default.Autorenew,
                        uiState.isAutoUpdateEnabled
                ) { viewModel.setAutoUpdateEnabled(context, it) }

                // Auto-download
                EnhancedSettingSwitch(
                        stringResource(R.string.auto_download_title),
                        stringResource(R.string.auto_download_desc),
                        Icons.Default.Download,
                        uiState.isAutoDownloadEnabled
                ) { viewModel.setAutoDownloadEnabled(context, it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseChannelDropdown(uiState: UpdateViewModel.UiState, viewModel: UpdateViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val channels =
            listOf(
                    Triple(
                            stringResource(R.string.channel_stable),
                            LocalUpdateManager.ReleaseChannel.STABLE,
                            stringResource(R.string.channel_stable_desc)
                    ),
                    Triple(
                            stringResource(R.string.channel_beta),
                            LocalUpdateManager.ReleaseChannel.BETA,
                            stringResource(R.string.channel_beta_desc)
                    ),
                    Triple(
                            stringResource(R.string.channel_alpha),
                            LocalUpdateManager.ReleaseChannel.ALPHA,
                            stringResource(R.string.channel_alpha_desc)
                    )
            )

    val selectedChannel = channels.find { it.second == uiState.selectedChannel }
    val selectedChannelName = selectedChannel?.first ?: stringResource(R.string.channel_stable)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
                text = stringResource(R.string.release_channel_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                    value = selectedChannelName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier =
                            Modifier.fillMaxWidth()
                                    .menuAnchor(
                                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                            enabled = true
                                    ),
                    shape = MaterialTheme.shapes.large,
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
            )

            ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.large
            ) {
                channels.forEach { (name, channel, description) ->
                    DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                            name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                            description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setSelectedChannel(context, channel)
                                expanded = false
                            },
                            leadingIcon = {
                                RadioButton(
                                        selected = uiState.selectedChannel == channel,
                                        onClick = null
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualActionsContent(uiState: UpdateViewModel.UiState, viewModel: UpdateViewModel) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pulsante principale di controllo aggiornamenti
        EnhancedActionButton(
                stringResource(R.string.check_updates_button),
                Icons.Default.Search,
                { viewModel.checkForUpdates(context) },
                enabled = uiState.updateCheckState !is UpdateViewModel.UpdateCheckState.Checking,
                showProgress = uiState.updateCheckState is UpdateViewModel.UpdateCheckState.Checking
        )

        // Pulsante download (solo se disponibile un aggiornamento)
        AnimatedVisibility(
                visible =
                        uiState.updateCheckState is
                                UpdateViewModel.UpdateCheckState.UpdateAvailable,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
        ) {
            if (uiState.updateCheckState is UpdateViewModel.UpdateCheckState.UpdateAvailable) {
                EnhancedActionButton(
                        stringResource(R.string.download_update_button),
                        Icons.Default.Download,
                        { viewModel.downloadUpdate(context, uiState.updateCheckState.updateInfo) }
                )
            }
        }

        // Barra di progresso download (solo durante il download)
        AnimatedVisibility(
                visible = uiState.downloadState is UpdateViewModel.DownloadState.Downloading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
        ) {
            if (uiState.downloadState is UpdateViewModel.DownloadState.Downloading) {
                val progress = uiState.downloadState.progress

                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                        shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                                text = stringResource(R.string.downloading_progress),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                        )

                        LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "$progress%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                    text = stringResource(R.string.downloading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogContent(uiState: UpdateViewModel.UiState) {
    val changelog =
            (uiState.updateCheckState as? UpdateViewModel.UpdateCheckState.UpdateAvailable)
                    ?.updateInfo
                    ?.changelog
    if (changelog.isNullOrEmpty()) return

    changelog.take(3).forEachIndexed { index, entry ->
        AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
        ) {
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor =
                                            MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                                Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                        )
                        Text(
                                text =
                                        stringResource(
                                                R.string.changelog_version,
                                                entry.version,
                                                entry.date
                                        ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }

                    entry.changes.forEach { change ->
                        Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                    "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                    change,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (index < changelog.take(3).size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ----------------- ENHANCED COMPONENTS -----------------

@Composable
fun DynamicThemeContent(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(stringResource(R.string.dynamic_theme_title), fontWeight = FontWeight.Medium)
            Text(
                    stringResource(R.string.dynamic_theme_desc),
                    style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
fun EnhancedUpdateItem(
        icon: ImageVector,
        title: String,
        description: String,
        color: Color,
        showProgress: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by
            infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (showProgress) 1.05f else 1f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(1000, easing = EaseInOutCubic),
                                    repeatMode = RepeatMode.Reverse
                            ),
                    label = "scale"
            )

    Card(
            modifier = Modifier.fillMaxWidth().scale(scale),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                )
            }

            if (showProgress) {
                Spacer(Modifier.width(16.dp))
                CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = color,
                        strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun EnhancedChannelItem(
        title: String,
        description: String,
        selected: Boolean,
        onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by
            animateColorAsState(
                    targetValue =
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                    label = "background"
            )

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.98f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                    label = "scale"
            )

    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .scale(scale)
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onClick
                            ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                )
                Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (selected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.8f
                                        )
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                )
            }

            RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors =
                            RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.outline
                            )
            )
        }
    }
}

@Composable
fun EnhancedActionButton(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit,
        enabled: Boolean = true,
        showProgress: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed && enabled) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                    label = "scale"
            )

    Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().scale(scale).height(56.dp),
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation =
                    ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }

        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EnhancedInfoItem(
        icon: ImageVector,
        title: String,
        value: String,
        isCopyable: Boolean = false,
        context: Context
) {
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by
            animateColorAsState(
                    targetValue =
                            if (isPressed && isCopyable) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                    label = "background"
            )

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed && isCopyable) 0.98f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                    label = "scale"
            )

    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .scale(scale)
                            .clickable(
                                    enabled = isCopyable,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        val clipboard =
                                                context.getSystemService(
                                                        Context.CLIPBOARD_SERVICE
                                                ) as
                                                        ClipboardManager
                                        clipboard.setPrimaryClip(
                                                ClipData.newPlainText(title, value)
                                        )
                                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT)
                                                .show()
                                    }
                            ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                )
            }

            if (isCopyable) {
                Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
