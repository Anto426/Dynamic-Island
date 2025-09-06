package com.anto426.dynamicisland.plugins.notification

import android.app.Notification
import android.app.RemoteInput
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.anto426.dynamicisland.model.ACTION_OPEN_CLOSE
import com.anto426.dynamicisland.model.NOTIFICATION_POSTED
import com.anto426.dynamicisland.model.NOTIFICATION_REMOVED
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginPriority
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import androidx.core.content.ContextCompat
import com.skydoves.landscapist.rememberDrawablePainter
import com.anto426.dynamicisland.ui.island.RoundedPainterIcon
import com.anto426.dynamicisland.ui.island.SectionCard
import com.anto426.dynamicisland.ui.island.PluginDefaults
import kotlinx.coroutines.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import com.anto426.dynamicisland.R

class NotificationPlugin(
	override val id: String = "NotificationPlugin",
	override val name: String = "Notification",
	override val description: String = "Mostra le notifiche e permette di interagire con esse (rispondere, aprire, ecc.).",
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override val permissions: ArrayList<String> = arrayListOf(
		Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
	),
	override var pluginSettings: SnapshotStateMap<String, PluginSettingsItem> = mutableStateMapOf(),
	override val version: String = "1.0.0",
	override val author: String = "Anto426", override val sourceCodeUrl: String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/notification/NotificationPlugin.kt",
) : BasePlugin() {
	override val nameRes: Int? get() = R.string.plugin_notification_name
	override val descriptionRes: Int? get() = R.string.plugin_notification_description

	private companion object {
		private const val TAG = "NotificationPlugin"
	}

	private lateinit var context: IslandOverlayService
	// Resolve service dynamically to avoid stale/null instance
	private val notificationService get() = NotificationService.getInstance()
	private var notificationMeta by mutableStateOf<NotificationMeta?>(null)

	private val pluginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private var autoHideJob: Job? = null
	private val DEFAULT_AUTO_HIDE_MS = 4000L

	private fun startAutoHide(timeoutMs: Long = DEFAULT_AUTO_HIDE_MS) {
		// Respect user setting
		if (!isAutoHideEnabled(context)) return
		autoHideJob?.cancel()
		autoHideJob = pluginScope.launch {
			try {
				delay(timeoutMs)
				// Auto-hide only if still showing same notification
				if (notificationMeta != null) hide(context)
			} catch (_: CancellationException) {}
		}
	}

	private fun isAutoHideEnabled(context: Context): Boolean {
		val key = "notification_auto_hide_enabled"
		val item = pluginSettings[key] as? PluginSettingsItem.SwitchSettingsItem
		return item?.isSettingEnabled(context, key) ?: true
	}

	private fun formatRelativeTime(timeMs: Long?): String {
		if (timeMs == null || timeMs <= 0) return ""
		val now = System.currentTimeMillis()
		val diff = now - timeMs
		val minute = 60_000L
		val hour = 60 * minute
		return when {
			diff < minute -> context.getString(R.string.notification_now)
			diff < hour -> "${diff / minute}m"
			else -> "${diff / hour}h"
		}
	}

	private fun testNotification() {
		Log.d(TAG, "Testing notification display")
		// Create a test notification meta
		val testIcon = context.packageManager.getApplicationIcon(context.packageName)
		notificationMeta = NotificationMeta(
			title = "Test Notification",
			body = "This is a test notification to verify the UI works correctly",
			id = 999,
			iconDrawable = testIcon,
			packageName = context.packageName,
			actions = emptyList(),
			statusBarNotification = null
		)
	// Start auto-hide via manager as well as local backup
		val timeout = if (isAutoHideEnabled(context)) DEFAULT_AUTO_HIDE_MS else 0L
		startAutoHide(timeout)
	// Force highest priority for notifications
	priority.value = PluginPriority.CRITICAL
		Log.d(TAG, "Showing test notification")
		show(context, timeoutMs = timeout)
	}

	private val notificationBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			Log.d(TAG, "BroadcastReceiver onReceive called with action: ${intent.action}")
			val extras: Bundle = intent.extras ?: return
			Log.d(TAG, "Plugin received broadcast: ${intent.action}")
			when (intent.action) {
				NOTIFICATION_POSTED -> {
					// Get the latest notification (should be the one that just arrived)
					val sbn = notificationService?.notifications?.lastOrNull() ?: return
					Log.d(TAG, "Notifica ricevuta: ${sbn.notification.extras.getString(Notification.EXTRA_TITLE)} from ${sbn.packageName}")
					notificationMeta = NotificationMeta(
						title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
						body = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
						id = sbn.id,
						iconDrawable = sbn.notification.smallIcon.loadDrawable(context) ?: return,
						packageName = sbn.packageName,
						actions = (sbn.notification.actions ?: arrayOf()).toList(),
						statusBarNotification = sbn
					)
					// Start auto-hide timer for notifications
					val timeout = if (isAutoHideEnabled(context)) DEFAULT_AUTO_HIDE_MS else 0L
					startAutoHide(timeout)
					// Force highest priority for notifications
					priority.value = PluginPriority.CRITICAL
					Log.d(TAG, "Showing notification plugin")
					this@NotificationPlugin.show(this@NotificationPlugin.context, timeoutMs = timeout)
				}
				"com.anto426.dynamicisland.TEST_NOTIFICATION" -> {
					// Test action for debugging
					Log.d(TAG, "Test notification requested")
					testNotification()
				}
				NOTIFICATION_REMOVED -> {
					val removedId = extras.getInt("id")
					val key = extras.getString("key")
					if (notificationMeta?.statusBarNotification?.key == key || notificationMeta?.id == removedId) {
						Log.d(TAG, "Rimuovendo notifica attiva: $removedId")
						removeNotificationAndUpdateState(removedId, key)
					}
				}
			}
		}
	}

	private fun removeNotificationAndUpdateState(id: Int, key: String? = null) {
		notificationService?.notifications?.let { list ->
			if (!key.isNullOrEmpty()) {
				list.removeAll { it.key == key }
			} else {
				list.removeAll { it.id == id }
			}
		}
		val nextSbn = notificationService?.notifications?.firstOrNull()
		notificationMeta = nextSbn?.let {
			NotificationMeta(
				title = it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
				body = it.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
				id = it.id,
				iconDrawable = it.notification.smallIcon.loadDrawable(context) ?: return@let null,
				packageName = it.packageName,
				actions = (it.notification.actions ?: emptyArray()).toList(),
				statusBarNotification = it
			)
		}

		if (notificationMeta == null) {
			Log.d(TAG, "Nessuna notifica rimasta, rimuovo il plugin.")
			this.hide(context)
		} else {
			// Autohide removed: keep showing next notification until user interaction or system removal
		}
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		Log.d(TAG, "NotificationPlugin onCreate called")
		// Ensure settings are initialized for UI and behavior
		initSettings(this.context)
		val filter = IntentFilter().apply {
			addAction(NOTIFICATION_POSTED)
			addAction(NOTIFICATION_REMOVED)
			addAction("com.anto426.dynamicisland.TEST_NOTIFICATION")
		}
		ContextCompat.registerReceiver(
			context.applicationContext,
			notificationBroadcastReceiver,
			filter,
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
		Log.d(TAG, "NotificationPlugin receiver registered with application context")

		// Test the notification UI immediately
		testNotification()
	}

	override fun initSettings(context: Context) {
		if (pluginSettings.isNotEmpty()) return
		pluginSettings["notification_auto_hide_enabled"] = PluginSettingsItem.SwitchSettingsItem(
			id = "notification_auto_hide_enabled",
			title = context.getString(R.string.auto_hide_title),
			description = context.getString(R.string.auto_hide_desc),
			value = mutableStateOf(true)
		)
		pluginSettings["notification_show_app_name"] = PluginSettingsItem.SwitchSettingsItem(
			id = "notification_show_app_name",
			title = context.getString(R.string.show_app_name_title),
			description = context.getString(R.string.show_app_name_desc),
			value = mutableStateOf(true)
		)
		pluginSettings["notification_show_body_compact"] = PluginSettingsItem.SwitchSettingsItem(
			id = "notification_show_body_compact",
			title = context.getString(R.string.show_body_compact_title),
			description = context.getString(R.string.show_body_compact_desc),
			value = mutableStateOf(true)
		)
	}

	override fun onSettingsChanged(context: Context, key: String, value: Any?) {
		when (key) {
			"notification_auto_hide_enabled",
			"notification_show_app_name",
			"notification_show_body_compact" ->
				(pluginSettings[key] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value = (value as? Boolean) ?: true
		}
	}

	@Composable
	override fun Composable() {
		val meta = notificationMeta ?: return

		// Read UI-related settings reactively
		val showAppName by remember {
			derivedStateOf {
				(pluginSettings["notification_show_app_name"] as? PluginSettingsItem.SwitchSettingsItem)
					?.value?.value ?: true
			}
		}

		// Main card with consistent padding and elevation
		Card(
			modifier = Modifier
				.fillMaxSize()
				.padding(8.dp),
			shape = RoundedCornerShape(24.dp),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
			),
			elevation = CardDefaults.cardElevation(
				defaultElevation = 8.dp,
				pressedElevation = 4.dp
			)
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(PluginDefaults.ContentPadding),
				horizontalAlignment = Alignment.Start,
				verticalArrangement = Arrangement.spacedBy(16.dp)
			) {
				// Header with app icon and text
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.Top,
					horizontalArrangement = Arrangement.spacedBy(16.dp)
				) {
					RoundedPainterIcon(
						painter = rememberDrawablePainter(drawable = meta.iconDrawable),
						size = 56.dp,
						iconSize = 28.dp,
						backgroundColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
					)

					// Title and body
					Column(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(6.dp)
					) {
						Text(
							text = meta.title ?: stringResource(R.string.notification_default_title),
							style = MaterialTheme.typography.headlineSmall,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							lineHeight = 24.sp
						)

						if (meta.body.isNotBlank()) {
							Text(
								text = meta.body,
								style = MaterialTheme.typography.bodyLarge,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								maxLines = 3,
								overflow = TextOverflow.Ellipsis,
								lineHeight = 20.sp
							)
						}

						// App source
						if (showAppName) {
							Text(
								text = meta.getAppName(context),
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.primary,
								fontWeight = FontWeight.Medium,
								modifier = Modifier.padding(top = 4.dp)
							)
						}
					}

					// Relative time label
					val postTime = meta.statusBarNotification?.postTime
					val timeLabel = remember(postTime) { formatRelativeTime(postTime) }
					if (timeLabel.isNotEmpty()) {
						Text(
							text = timeLabel,
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}

				// Divider elegante
				HorizontalDivider(
					modifier = Modifier.padding(vertical = 8.dp),
					color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
				)

				// Actions
				if (meta.actions.isNotEmpty()) {
					SectionCard(
						title = stringResource(R.string.actions_title),
						modifier = Modifier.fillMaxWidth(),
						containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
					) {
						NotificationActions(meta)
					}
				}

				// Indicatore di swipe migliorato
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					horizontalArrangement = Arrangement.Center,
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
						contentDescription = stringResource(R.string.notification_swipe_left_to_close),
						tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
						modifier = Modifier.size(14.dp)
					)
					Text(
						text = stringResource(R.string.notification_swipe_to_close),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
						fontWeight = FontWeight.Medium
					)
				}
			}
		}
	}


	@Composable
	private fun NotificationActions(meta: NotificationMeta) {
		var isReplying by remember { mutableStateOf(false) }
		var replyText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
		val focusRequester = remember { FocusRequester() }

		LaunchedEffect(isReplying) {
			if (isReplying) focusRequester.requestFocus()
		}

		if (isReplying) {
			val replyAction = meta.actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
			if (replyAction != null) {
				// Reply mode con design elegante
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					// TextField migliorato
					OutlinedTextField(
						value = replyText,
						onValueChange = { replyText = it },
						modifier = Modifier
							.fillMaxWidth()
							.focusRequester(focusRequester),
						placeholder = {
							Text(
								replyAction.remoteInputs.first().label.toString(),
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						},
						shape = RoundedCornerShape(16.dp),
						colors = OutlinedTextFieldDefaults.colors(
							focusedBorderColor = MaterialTheme.colorScheme.primary,
							unfocusedBorderColor = MaterialTheme.colorScheme.outline,
							focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
							unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
						),
						textStyle = MaterialTheme.typography.bodyLarge,
						minLines = 2,
						maxLines = 4
					)

					// Bottoni di azione eleganti
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
					) {
						// Bottone annulla
						TextButton(
							onClick = { isReplying = false },
							modifier = Modifier.height(40.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Text(
								"Cancel",
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.Medium
							)
						}

						// Bottone invia
						FilledTonalButton(
							onClick = {
								autoHideJob?.cancel()
								sendReply(replyAction, replyText.text)
								removeNotificationAndUpdateState(meta.id,
                                    meta.statusBarNotification?.key
                                )
							},
							modifier = Modifier.height(40.dp),
							shape = RoundedCornerShape(12.dp),
							enabled = replyText.text.isNotBlank()
						) {
							Icon(
								Icons.AutoMirrored.Filled.Send,
								contentDescription = stringResource(R.string.send_reply),
								modifier = Modifier.size(16.dp)
							)
							Spacer(modifier = Modifier.width(6.dp))
							Text(
								stringResource(R.string.send_reply),
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.Medium
							)
						}
					}
				}
			}
		} else {
			// Modalità normale con bottoni eleganti
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				meta.actions.chunked(2).forEach { rowActions ->
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						rowActions.forEach { action ->
							val isReplyAction = action.remoteInputs?.isNotEmpty() == true

							if (isReplyAction) {
								// Bottone reply speciale
								FilledTonalButton(
									onClick = {
										autoHideJob?.cancel()
										isReplying = true
									},
									modifier = Modifier
										.weight(1f)
										.height(44.dp),
									shape = RoundedCornerShape(14.dp),
									colors = ButtonDefaults.filledTonalButtonColors(
										containerColor = MaterialTheme.colorScheme.secondaryContainer,
										contentColor = MaterialTheme.colorScheme.onSecondaryContainer
									)
								) {
									Icon(
										Icons.AutoMirrored.Filled.Chat,
										contentDescription = null,
										modifier = Modifier.size(18.dp)
									)
									Spacer(modifier = Modifier.width(6.dp))
									Text(
										text = action.title.toString(),
										style = MaterialTheme.typography.labelLarge,
										fontWeight = FontWeight.Medium
									)
								}
							} else {
								// Altri bottoni
								OutlinedButton(
									onClick = {
										autoHideJob?.cancel()
										action.actionIntent.send()
										removeNotificationAndUpdateState(meta.id,
                                            meta.statusBarNotification?.key
                                        )
									},
									modifier = Modifier
										.weight(1f)
										.height(44.dp),
									shape = RoundedCornerShape(14.dp),
									border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
								) {
									Text(
										text = action.title.toString(),
										style = MaterialTheme.typography.labelLarge,
										fontWeight = FontWeight.Medium
									)
								}
							}
						}
					}
				}
			}
		}
	}

	private fun sendReply(action: Notification.Action, text: String) {
		val remoteInput = action.remoteInputs?.firstOrNull() ?: return
		val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
		val bundle = Bundle().apply { putCharSequence(remoteInput.resultKey, text) }
		RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
		action.actionIntent.send(context, 0, intent)
	}

	override fun onClick() {
		autoHideJob?.cancel() // Cancel auto-hide on user interaction
		notificationMeta?.let {
			context.sendBroadcast(Intent(ACTION_OPEN_CLOSE).putExtra("id", it.id).putExtra("key",
                it.statusBarNotification?.key
            ))
		}
	}

	override fun onDestroy() {
		if (!::context.isInitialized) return
		autoHideJob?.cancel()
		try { context.unregisterReceiver(notificationBroadcastReceiver) } catch (e: IllegalArgumentException) { }
		pluginScope.cancel()
	}

	@Composable override fun PermissionsRequired() { }

	@Composable
	override fun LeftOpenedComposable() {
		notificationMeta?.let { meta ->
			RoundedPainterIcon(
				painter = rememberDrawablePainter(drawable = meta.iconDrawable),
				size = 48.dp,
				iconSize = 24.dp,
				backgroundColor = MaterialTheme.colorScheme.primaryContainer,
				contentColor = MaterialTheme.colorScheme.onPrimaryContainer
			)
		}
	}

	@Composable
	override fun RightOpenedComposable() {
		val meta = notificationMeta ?: return
		val showBodyInCompact by remember {
			derivedStateOf {
				(pluginSettings["notification_show_body_compact"] as? PluginSettingsItem.SwitchSettingsItem)
					?.value?.value ?: true
			}
		}
		Column(
			modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = meta.title ?: stringResource(R.string.notification_default_title),
				style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 1,
				overflow = TextOverflow.Clip,
				modifier = Modifier.basicMarquee()
			)
			if (showBodyInCompact && meta.body.isNotBlank()) {
				Text(
					text = meta.body,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					overflow = TextOverflow.Clip,
					modifier = Modifier.basicMarquee()
				)
			}
		}
	}

	override fun onLeftSwipe() {
		autoHideJob?.cancel() // Cancel auto-hide on user interaction
		notificationMeta?.let { removeNotificationAndUpdateState(it.id,
            it.statusBarNotification?.key
        ) }
	}

	override fun onRightSwipe() {}

	// Auto-hide implemented: notifications dismiss after 4 seconds unless user interacts
}
// end class
