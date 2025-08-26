package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.rememberDrawablePainter
import com.anto426.dynamicisland.R
import java.text.SimpleDateFormat
import java.util.*

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
	val versionCode =
		packageInfo.longVersionCode
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
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(24.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Image(
						painter = appIcon,
						contentDescription = "Icona dell'app",
						modifier = Modifier
							.size(48.dp)
							.clip(CircleShape)
					)
					Spacer(Modifier.height(8.dp))
					Text(appName, style = MaterialTheme.typography.titleLarge)
					Text(
						packageName,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
		item {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							text = "Informazioni sull'App",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
						InfoItem(
							icon = Icons.Default.Info,
							title = "Versione",
							value = "$versionName ($versionCode)",
							isCopyable = true,
							context = context
						)
						SettingsDivider()
						InfoItem(
							icon = Icons.Default.Build,
							title = "SDK di destinazione",
							value = "$targetSdk",
							isCopyable = true,
							context = context
						)
						SettingsDivider()
						InfoItem(
							icon = Icons.Default.SystemUpdate,
							title = "SDK minimo",
							value = "$minSdk",
							isCopyable = true,
							context = context
						)
						SettingsDivider()
						InfoItem(
							icon = Icons.Default.DateRange,
							title = "Data di compilazione",
							value = buildDate,
							isCopyable = true,
							context = context
						)
					}
					SettingsDivider()
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							text = "Informazioni sul Dispositivo",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
						InfoItem(
							icon = Icons.Default.PhoneAndroid,
							title = "Versione Android",
							value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
							isCopyable = true,
							context = context
						)
						SettingsDivider()
						InfoItem(
							icon = Icons.Default.Memory,
							title = "Architettura CPU",
							value = Build.SUPPORTED_ABIS.joinToString(", "),
							isCopyable = true,
							context = context
						)
					}
					SettingsDivider()
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							text = "Link Utili",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
						InfoItem(
							icon = Icons.Default.Code,
							title = "Sviluppatore",
							value = "Anto426",
							onClick = {
								val url = context.getString(R.string.developer)
								uriHandler.openUri(url)
							},
							context = context						)
						SettingsDivider()
						InfoItem(
							icon = Icons.Default.BugReport,
							title = "GitHub",
							value = "Apri nel browser",
							onClick = {
								val url = context.getString(R.string.repositories)
								uriHandler.openUri(url)
							},
							context = context						)
					}
				}
			}
		}
	}
}

@Composable
fun InfoItem(
	icon: ImageVector,
	title: String,
	value: String,
	isCopyable: Boolean = false,
	context: Context? = null,
	onClick: (() -> Unit)? = null
) {
	val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
	val isClickable = isCopyable || onClick != null

	val onAction: () -> Unit = {
		when {
			isCopyable && clipboardManager != null -> {
				clipboardManager.setPrimaryClip(ClipData.newPlainText(title, value))
				Toast.makeText(context, "$title copiato!", Toast.LENGTH_SHORT).show()
			}
			onClick != null -> onClick.invoke()
		}
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.clickable(enabled = isClickable) { onAction() }
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSecondaryContainer,
			modifier = Modifier.size(24.dp)
		)
		Spacer(Modifier.width(16.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyMedium
			)
			Text(
				text = value,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}