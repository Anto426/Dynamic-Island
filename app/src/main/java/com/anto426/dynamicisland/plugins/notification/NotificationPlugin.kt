package com.anto426.dynamicisland.plugins.notification

import android.app.Notification
import android.app.RemoteInput
import android.content.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.model.ACTION_OPEN_CLOSE
import com.anto426.dynamicisland.model.NOTIFICATION_POSTED
import com.anto426.dynamicisland.model.NOTIFICATION_REMOVED
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.skydoves.landscapist.rememberDrawablePainter
import kotlinx.coroutines.*

class NotificationPlugin(
	override val id: String = "NotificationPlugin",
	override val name: String = "Notification",
	override val description: String = "Mostra le notifiche e permette di interagire con esse (rispondere, aprire, ecc.).",
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override val permissions: ArrayList<String> = arrayListOf(
		Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
	),
	override var pluginSettings: MutableMap<String, PluginSettingsItem> = mutableMapOf(),
	override val version: String = "1.0.0",
	override val author: String = "Anto426", override val sourceCodeUrl: String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/notification/NotificationPlugin.kt",
) : BasePlugin() {

	private companion object {
		private const val TAG = "NotificationPlugin"
	}

	private lateinit var context: IslandOverlayService
	private val notificationService = NotificationService.getInstance()
	private var notificationMeta by mutableStateOf<NotificationMeta?>(null)

	private val pluginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private var dismissJob: Job? = null

	private val notificationBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val extras: Bundle = intent.extras ?: return
			when (intent.action) {
				NOTIFICATION_POSTED -> {
					val notificationId = extras.getInt("id")
					val sbn = notificationService?.notifications?.lastOrNull { it.id == notificationId } ?: return
					Log.d(TAG, "Notifica ricevuta: ${sbn.notification.extras.getString(Notification.EXTRA_TITLE)}")
					notificationMeta = NotificationMeta(
						title = sbn.notification.extras.getString(Notification.EXTRA_TITLE),
						body = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: "",
						id = sbn.id,
						iconDrawable = sbn.notification.smallIcon.loadDrawable(context) ?: return,
						packageName = sbn.packageName,
						actions = (sbn.notification.actions ?: arrayOf()).toList(),
						statusBarNotification = sbn
					)
					startDismissTimeout()
					this@NotificationPlugin.context.addPlugin(this@NotificationPlugin)
				}
				NOTIFICATION_REMOVED -> {
					val removedId = extras.getInt("id")
					if (notificationMeta?.id == removedId) {
						Log.d(TAG, "Rimuovendo notifica attiva: $removedId")
						removeNotificationAndUpdateState(removedId)
					}
				}
			}
		}
	}

	private fun removeNotificationAndUpdateState(id: Int) {
		notificationService?.notifications?.removeAll { it.id == id }
		dismissJob?.cancel()
		val nextSbn = notificationService?.notifications?.firstOrNull()
		notificationMeta = nextSbn?.let {
			NotificationMeta(
				title = it.notification.extras.getString(Notification.EXTRA_TITLE),
				body = it.notification.extras.getString(Notification.EXTRA_TEXT) ?: "",
				id = it.id,
				iconDrawable = it.notification.smallIcon.loadDrawable(context) ?: return@let null,
				packageName = it.packageName,
				actions = (it.notification.actions ?: emptyArray()).toList(),
				statusBarNotification = it
			)
		}

		if (notificationMeta == null) {
			Log.d(TAG, "Nessuna notifica rimasta, rimuovo il plugin.")
			context.removePlugin(this)
		} else {
			startDismissTimeout()
		}
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		val filter = IntentFilter().apply {
			addAction(NOTIFICATION_POSTED)
			addAction(NOTIFICATION_REMOVED)
		}
		context.registerReceiver(notificationBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
	}

	@Composable
	override fun Composable() {
		TODO("Not yet implemented")
	}


	@Composable
	private fun NotificationActions(meta: NotificationMeta) {
		var isReplying by remember { mutableStateOf(false) }
		var replyText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
		val focusRequester = remember { FocusRequester() }

		LaunchedEffect(isReplying) {
			if (isReplying) focusRequester.requestFocus()
		}

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
		) {
			val replyAction = meta.actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }

			if (isReplying && replyAction != null) {
				OutlinedTextField(
					value = replyText,
					onValueChange = { replyText = it },
					modifier = Modifier.weight(1f).focusRequester(focusRequester),
					placeholder = { Text(replyAction.remoteInputs.first().label.toString()) },
					shape = CircleShape
				)
				IconButton(onClick = {
					sendReply(replyAction, replyText.text)
					removeNotificationAndUpdateState(meta.id)
				}) {
					Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Invia risposta")
				}
			} else {
				meta.actions.forEach { action ->
					Button(
						modifier = Modifier.weight(1f),
						onClick = {
							if (action.remoteInputs?.isNotEmpty() == true) {
								isReplying = true
							} else {
								action.actionIntent.send()
								removeNotificationAndUpdateState(meta.id)
							}
						}
					) {
						Text(text = action.title.toString())
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
		notificationMeta?.let {
			context.sendBroadcast(Intent(ACTION_OPEN_CLOSE).putExtra("id", it.id))
		}
	}

	override fun onDestroy() {
		if (!::context.isInitialized) return
		try { context.unregisterReceiver(notificationBroadcastReceiver) } catch (e: IllegalArgumentException) { }
		pluginScope.cancel()
	}

	@Composable override fun PermissionsRequired() { }

	@Composable
	override fun LeftOpenedComposable() {
		notificationMeta?.let { meta ->
			Box(
				modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
			) {
				Icon(
					painter = rememberDrawablePainter(drawable = meta.iconDrawable),
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
					contentDescription = null,
					modifier = Modifier.padding(4.dp)
				)
			}
		}
	}

	@Composable
	override fun RightOpenedComposable() {
		Box(
			modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.Chat,
				tint = MaterialTheme.colorScheme.onSecondaryContainer,
				contentDescription = null,
				modifier = Modifier.padding(4.dp)
			)
		}
	}

	override fun onLeftSwipe() {
		notificationMeta?.let { removeNotificationAndUpdateState(it.id) }
	}

	override fun onRightSwipe() {}

	private fun startDismissTimeout() {
		dismissJob?.cancel()
		dismissJob = pluginScope.launch {
			delay(IslandSettings.instance.autoHideOpenedAfter.toLong())
			notificationMeta?.let { removeNotificationAndUpdateState(it.id) }
		}
	}
}
