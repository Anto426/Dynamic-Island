package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.anto426.dynamicisland.plugins.ExportedPlugins
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Intent
import com.anto426.dynamicisland.model.SETTINGS_CHANGED

/**
 * Componente per gli elementi informativi dei plugin con design moderno e ottimizzato
 */
@Composable
fun PluginInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    isCopyable: Boolean = false,
    onClick: (() -> Unit)? = null,
    context: Context? = null
) {
    val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val isClickable = isCopyable || onClick != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && isClickable) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
        label = "background"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isClickable) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100),
        label = "scale"
    )

    val onAction: () -> Unit = {
        when {
            onClick != null -> onClick.invoke()
            isCopyable && clipboardManager != null -> {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(title, value))
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.copied_to_clipboard, title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(backgroundColor, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (isClickable) {
                    Modifier.clickable(
                        enabled = isClickable,
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onAction
                    )
                } else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 18.sp
            )
        }

        if (isClickable) {
            Icon(
                imageVector = if (isCopyable) Icons.Default.ContentCopy else Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = if (isCopyable)
                    stringResource(id = R.string.copy_title, title)
                else
                    stringResource(id = R.string.open_link),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Titolo del gruppo con miglior spaziatura
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                content()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSettingsScreen(
    plugin: BasePlugin
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    // Ensure plugin settings are initialized so they render even if plugin isn't started
    LaunchedEffect(plugin, context) {
        runCatching { plugin.initSettings(context) }
    }

    // Aggiorna lo stato dei permessi quando si torna dalla schermata di sistema
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ExportedPlugins.setupPlugins(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Icona del plugin migliorata
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = stringResource(id = R.string.plugin_icon_content_description),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = plugin.nameRes?.let { stringResource(id = it) } ?: plugin.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Text(
                        text = stringResource(id = R.string.plugin_author_prefix) + plugin.author,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // Descrizione con miglior leggibilità
                    Text(
                        text = plugin.descriptionRes?.let { stringResource(id = it) }
                            ?: plugin.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        item {
            SettingGroup(
                title = stringResource(id = R.string.plugin_status_title)
            ) {
                EnhancedSettingSwitch(
                    title = stringResource(id = R.string.plugin_enable_title),
                    description = if (plugin.allPermissionsGranted)
                        stringResource(id = R.string.plugin_enable_description_generic)
                    else
                        stringResource(id = R.string.plugin_enable_description_needs_permissions),
                    icon = Icons.Default.PowerSettingsNew,
                    checked = plugin.enabled.value,
                    onCheckedChange = { plugin.switchEnabled(context) }
                )
            }
        }

        // Informazioni plugin
        item {
            SettingGroup(
                title = stringResource(id = R.string.settings_section_information)
            ) {
                PluginInfoItem(
                    icon = Icons.Default.Info,
                    title = stringResource(id = R.string.plugin_version_title),
                    value = plugin.version,
                    isCopyable = true,
                    context = context
                )

                SettingsDivider()

                PluginInfoItem(
                    icon = Icons.Default.Fingerprint,
                    title = stringResource(id = R.string.plugin_identifier_title),
                    value = plugin.id,
                    isCopyable = true,
                    context = context
                )

                run {
                    val url = plugin.sourceCodeUrl.toString()
                    if (url.isNotBlank()) {
                        SettingsDivider()
                        PluginInfoItem(
                            icon = Icons.Default.Code,
                            title = stringResource(id = R.string.plugin_source_code_title),
                            value = stringResource(id = R.string.plugin_view_on_web),
                            onClick = {
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_open_link),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            context = context
                        )
                    }
                }
            }
        }

        if (plugin.permissions.isNotEmpty()) {
            item {
                SettingGroup(
                    title = stringResource(id = R.string.plugin_permissions_section)
                ) {
                    plugin.permissions.forEachIndexed { index, permissionKey ->
                        if (permissionKey.isNotBlank()) {
                            val perm = ExportedPlugins.permissions[permissionKey]
                            val granted = perm?.granted?.value == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let {
                                        if (!granted && perm != null) {
                                            it.clickable {
                                                try {
                                                    context.startActivity(
                                                        perm.requestIntent.addFlags(
                                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                }
                                            }
                                        } else it
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Spunta stato permesso
                                Icon(
                                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Circle,
                                    contentDescription = if (granted)
                                        stringResource(id = R.string.permission_granted_label)
                                    else
                                        stringResource(id = R.string.permission_not_granted_label),
                                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(22.dp)
                                )

                                val permName = perm?.name ?: permissionKey
                                val permDesc = perm?.description ?: ""
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = permName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = permDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }

                                if (!granted && perm != null) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(id = R.string.open_permission_settings),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (index < plugin.permissions.size - 1) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }

        // Impostazioni specifiche del plugin con miglior gestione errori
        if (plugin.pluginSettings.isNotEmpty()) {
            item {
                SettingGroup(
                    title = stringResource(id = R.string.plugin_settings_title)
                ) {
                    plugin.pluginSettings.values.forEachIndexed { index, settings ->
                        when (settings) {
                            is PluginSettingsItem.SwitchSettingsItem -> {
                                // Legge il valore persistito e sincronizza lo stato della UI
                                var checked by remember(settings.id) {
                                    mutableStateOf(settings.isSettingEnabled(context, settings.id))
                                }
                                EnhancedSettingSwitch(
                                    title = settings.title,
                                    description = settings.description,
                                    icon = Icons.Default.Settings,
                                    checked = checked,
                                    onCheckedChange = { newValue ->
                                        try {
                                            settings.onValueChange(context, newValue)
                                            // Notify plugin and service for real-time refresh
                                            runCatching {
                                                plugin.onSettingsChanged(
                                                    context,
                                                    settings.id,
                                                    newValue
                                                )
                                            }
                                            context.sendBroadcast(Intent(SETTINGS_CHANGED))
                                            // Aggiorna lo stato locale per un feedback immediato
                                            checked = newValue
                                        } catch (e: Exception) {
                                            val msg = e.localizedMessage
                                                ?: context.getString(R.string.unknown_error)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_error_prefix, msg),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }

                        if (index < plugin.pluginSettings.values.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            }
        }
    }
}

