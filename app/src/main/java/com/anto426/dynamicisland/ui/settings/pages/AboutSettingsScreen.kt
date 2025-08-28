package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.anto426.dynamicisland.R
import com.skydoves.landscapist.rememberDrawablePainter
import java.text.SimpleDateFormat
import java.util.*

import com.anto426.dynamicisland.ui.settings.pages.SettingsDivider
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val appIcon = rememberDrawablePainter(packageInfo.applicationInfo?.loadIcon(packageManager))

    val appName = context.applicationInfo.loadLabel(packageManager).toString()
    val packageName = context.packageName
    val versionName = packageInfo.versionName
    val versionCode = packageInfo.longVersionCode
    val targetSdk = context.applicationInfo.targetSdkVersion
    val minSdk = context.applicationInfo.minSdkVersion

    val buildDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
        Date(context.applicationInfo.sourceDir.let {
            java.io.File(it).lastModified()
        })
    )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Header dell'app
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icona dell'app
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        ) {
                            Image(
                                painter = appIcon,
                                contentDescription = stringResource(R.string.about_app_icon_description),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }

                        // Nome e versione
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Versione $versionName",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Informazioni sull'app
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Informazioni App",
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
                            EnhancedInfoItem(
                                icon = Icons.Default.Info,
                                title = stringResource(R.string.about_version),
                                value = "$versionName ($versionCode)",
                                isCopyable = true,
                                context = context
                            )

                            SettingsDivider()

                            EnhancedInfoItem(
                                icon = Icons.Default.Build,
                                title = stringResource(R.string.about_target_sdk),
                                value = "$targetSdk",
                                isCopyable = true,
                                context = context
                            )

                            SettingsDivider()

                            EnhancedInfoItem(
                                icon = Icons.Default.SystemUpdate,
                                title = stringResource(R.string.about_min_sdk),
                                value = "$minSdk",
                                isCopyable = true,
                                context = context
                            )

                            SettingsDivider()

                            EnhancedInfoItem(
                                icon = Icons.Default.DateRange,
                                title = stringResource(R.string.about_build_date),
                                value = buildDate,
                                isCopyable = true,
                                context = context
                            )
                        }
                    }
                }
            }

            // Informazioni dispositivo
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Dispositivo",
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
                            EnhancedInfoItem(
                                icon = Icons.Default.PhoneAndroid,
                                title = stringResource(R.string.about_android_version),
                                value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                isCopyable = true,
                                context = context
                            )

                            SettingsDivider()

                            EnhancedInfoItem(
                                icon = Icons.Default.Memory,
                                title = stringResource(R.string.about_cpu_architecture),
                                value = Build.SUPPORTED_ABIS.joinToString(", "),
                                isCopyable = true,
                                context = context
                            )
                        }
                    }
                }
            }

            // Link utili
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Link Utili",
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
                            EnhancedInfoItem(
                                icon = Icons.Default.Code,
                                title = stringResource(R.string.about_developer),
                                value = stringResource(R.string.developer_name),
                                onClick = {
                                    val url = context.getString(R.string.developer)
                                    uriHandler.openUri(url)
                                },
                                context = context
                            )

                            SettingsDivider()

                            EnhancedInfoItem(
                                icon = Icons.Default.BugReport,
                                title = stringResource(R.string.about_github),
                                value = stringResource(R.string.about_open_in_browser),
                                onClick = {
                                    val url = context.getString(R.string.repositories)
                                    uriHandler.openUri(url)
                                },
                                context = context
                            )
                        }
                    }
                }
            }
        }

}

/**
 * Componente avanzato per gli elementi informativi con design moderno
 */
@Composable
fun EnhancedInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    isCopyable: Boolean = false,
    context: Context? = null,
    onClick: (() -> Unit)? = null
) {
    val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
    val isClickable = isCopyable || onClick != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && isClickable) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
        label = "background"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isClickable) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )

    val onAction: () -> Unit = {
        when {
            isCopyable && clipboardManager != null -> {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(title, value))
                Toast.makeText(context, context?.getString(R.string.about_copied_toast, title), Toast.LENGTH_SHORT).show()
            }
            onClick != null -> onClick.invoke()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(backgroundColor, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = isClickable,
                interactionSource = interactionSource,
                indication = null,
                onClick = onAction
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icona
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
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

        // Contenuto
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        // Icona azione
        if (isClickable) {
            Icon(
                imageVector = if (isCopyable) Icons.Default.ContentCopy else Icons.Default.OpenInNew,
                contentDescription = if (isCopyable) "Copia" else "Apri",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
