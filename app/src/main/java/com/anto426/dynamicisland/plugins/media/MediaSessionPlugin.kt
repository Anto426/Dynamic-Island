package com.anto426.dynamicisland.plugins.media

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

private object MediaPluginDefaults {
	const val TAG = "MediaSessionPlugin"
	val PlayerArtworkShape = RoundedCornerShape(24.dp)
	val BackgroundBlurRadius = 32.dp
}

class MediaSessionPlugin(
	override val author: String = "Anto426",
	override val description: String = "Show the current media session playing",
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override val id: String = "MediaSessionPlugin",
	override val name: String = "MediaSession",
	override val permissions: ArrayList<String> = arrayListOf(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
	override var pluginSettings: MutableMap<String, PluginSettingsItem> = mutableMapOf(),
	override val version: String = "1.0.0",
	override val sourceCodeUrl:String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/media/MediaSessionPlugin.kt"

) : BasePlugin() {

	lateinit var context: IslandOverlayService
	private lateinit var mediaSessionManager: MediaSessionManager
	private val callbackMap = mutableStateMapOf<String, MediaCallback>()

	val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

	private var activeCallback by mutableStateOf<MediaCallback?>(null)

	private val listenerForActiveSessions = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
		controllers?.forEach { registerController(it) }
		val activePackages = controllers?.map { it.packageName } ?: emptyList()
		callbackMap.keys.filterNot { it in activePackages }.forEach { packageName ->
			callbackMap.remove(packageName)?.let { it.mediaController.unregisterCallback(it) }
		}
		updateActiveMediaSession()
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
		val componentName = ComponentName(context, NotificationService::class.java)
		try {
			mediaSessionManager.addOnActiveSessionsChangedListener(listenerForActiveSessions, componentName)
			mediaSessionManager.getActiveSessions(componentName).forEach(::registerController)
			updateActiveMediaSession()
		} catch (e: SecurityException) {
			Log.e(MediaPluginDefaults.TAG, "Notification Listener permission not granted.", e)
		}
	}

	private fun registerController(controller: MediaController) {
		if (callbackMap.containsKey(controller.packageName)) return
		Log.d(MediaPluginDefaults.TAG, "Registering controller for ${controller.packageName}")
		val callback = MediaCallback(controller, this, ::updateActiveMediaSession)
		callbackMap[controller.packageName] = callback
		controller.registerCallback(callback)
		callback.initialUpdate()
	}

	fun updateActiveMediaSession() {
		val playingSession = callbackMap.values.firstOrNull { it.mediaStruct.isPlaying() }

		if (playingSession != null) {
			activeCallback = playingSession
			context.addPlugin(this)
		} else {
			val mostRecentActive = callbackMap.values.filter {
				// CORREZIONE 1: Aggiunto operatore safe-call (?.) e valore di default (?:)
				val state = it.mediaStruct.playbackState.value?.state ?: PlaybackState.STATE_NONE
				state != PlaybackState.STATE_NONE && state != PlaybackState.STATE_STOPPED
			}.maxByOrNull {
				// CORREZIONE 2: Aggiunto operatore safe-call (?.) e valore di default (?:)
				it.mediaStruct.playbackState.value?.lastPositionUpdateTime ?: 0L
			}

			if (mostRecentActive != null) {
				activeCallback = mostRecentActive
				context.addPlugin(this)
				mostRecentActive.startAutoHideJob()
			} else {
				activeCallback = null
				context.removePlugin(this)
			}
		}
	}


	override fun onClick() {
		activeCallback?.mediaController?.sessionActivity?.send(0)
	}

	override fun onDestroy() {
		pluginScope.cancel()
		if (::mediaSessionManager.isInitialized) {
			try { mediaSessionManager.removeOnActiveSessionsChangedListener(listenerForActiveSessions) }
			catch (e: Exception) { Log.w(MediaPluginDefaults.TAG, "Failed to remove listener", e) }
		}
		callbackMap.values.forEach { it.mediaController.unregisterCallback(it) }
		callbackMap.clear()
	}

	@SuppressLint("DefaultLocale")
	private fun formatTime(millis: Long): String {
		if (millis < 0) return "00:00"
		val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
		val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
		return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
	}

	@Composable
	override fun Composable() {
		val mediaCallback = activeCallback ?: return
		val mediaStruct by remember { derivedStateOf { mediaCallback.mediaStruct } }

		Box(modifier = Modifier.fillMaxSize()) {
			PlayerBackground(cover = mediaStruct.cover.value)
			Column(
				modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.SpaceAround
			) {
				PlayerArtwork(
					modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
					cover = mediaStruct.cover.value
				)
				TrackDetails(title = mediaStruct.title.value, artist = mediaStruct.artist.value)
				PlayerScrubber(mediaStruct = mediaStruct, transportControls = mediaCallback.mediaController.transportControls)
				PlayerControls(isPlaying = mediaStruct.isPlaying(), transportControls = mediaCallback.mediaController.transportControls)
			}
		}
	}

	@Composable
	private fun PlayerBackground(cover: Bitmap?) {
		if (cover != null) {
			Image(
				bitmap = cover.asImageBitmap(),
				contentDescription = "Blurred Background",
				modifier = Modifier.fillMaxSize().blur(radius = MediaPluginDefaults.BackgroundBlurRadius),
				contentScale = ContentScale.Crop
			)
			Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
		} else {
			Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
		}
	}

	@Composable
	private fun PlayerArtwork(modifier: Modifier = Modifier, cover: Bitmap?) {
		Card(modifier = modifier, shape = MediaPluginDefaults.PlayerArtworkShape, elevation = CardDefaults.cardElevation(8.dp)) {
			AnimatedContent(
				targetState = cover,
				label = "CoverArtAnimation",
				transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) }
			) { currentCover ->
				if (currentCover != null) {
					Image(
						bitmap = currentCover.asImageBitmap(),
						contentDescription = "Album Cover",
						modifier = Modifier.fillMaxSize(),
						contentScale = ContentScale.Crop
					)
				} else {
					Box(
						contentAlignment = Alignment.Center,
						modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
					) {
						Icon(Icons.Default.MusicNote, "No Cover", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
					}
				}
			}
		}
	}

	@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
	@Composable
	private fun TrackDetails(modifier: Modifier = Modifier, title: String, artist: String) {
		Column(
			modifier = modifier.fillMaxWidth(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			AnimatedContent(targetState = title, label = "TitleAnimation", transitionSpec = {
				slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
			}) { text ->
				Text(text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, modifier = Modifier.basicMarquee())
			}
			Spacer(modifier = Modifier.height(4.dp))
			AnimatedContent(targetState = artist, label = "ArtistAnimation", transitionSpec = {
				slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
			}) { text ->
				Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.basicMarquee())
			}
		}
	}

	@Composable
	private fun PlayerScrubber(mediaStruct: MediaStruct, transportControls: MediaController.TransportControls) {
		var sliderPosition by remember { mutableFloatStateOf(0f) }
		var isDragging by remember { mutableStateOf(false) }
		val duration by remember { mediaStruct.duration }
		// CORREZIONE 3: Aggiunto operatore safe-call (?.) e valore di default (?:)
		val elapsed by remember { derivedStateOf { mediaStruct.playbackState.value?.position ?: 0L } }

		LaunchedEffect(elapsed, isDragging) {
			if (!isDragging) {
				sliderPosition = if (duration > 0) elapsed.toFloat() / duration else 0f
			}
		}

		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Slider(
				value = sliderPosition,
				onValueChange = {
					isDragging = true
					sliderPosition = it
				},
				onValueChangeFinished = {
					transportControls.seekTo((sliderPosition * duration).roundToLong())
					isDragging = false
				},
				modifier = Modifier.fillMaxWidth(),
				colors = SliderDefaults.colors(
					thumbColor = MaterialTheme.colorScheme.primary,
					activeTrackColor = MaterialTheme.colorScheme.primary,
					inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
				)
			)
			Row(
				Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				val currentTime = if (isDragging) (sliderPosition * duration).toLong() else elapsed
				Text(
					formatTime(currentTime),
					style = MaterialTheme.typography.labelSmall.copy(
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				)
				Spacer(modifier = Modifier.weight(1f))
				Text(
					formatTime(duration),
					style = MaterialTheme.typography.labelSmall.copy(
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				)
			}
		}
	}

	@Composable
	private fun PlayerControls(isPlaying: Boolean, transportControls: MediaController.TransportControls) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 8.dp),
			Arrangement.SpaceEvenly,
			Alignment.CenterVertically
		) {
			// Pulsante precedente con feedback visivo
			FilledTonalIconButton(
				onClick = { transportControls.skipToPrevious() },
				modifier = Modifier.size(56.dp),
				colors = IconButtonDefaults.filledTonalIconButtonColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
				)
			) {
				Icon(
					Icons.Default.SkipPrevious,
					contentDescription = "Traccia precedente",
					Modifier.size(28.dp)
				)
			}

			// Pulsante play/pause principale
			FilledIconButton(
				onClick = { if (isPlaying) transportControls.pause() else transportControls.play() },
				modifier = Modifier.size(80.dp),
				colors = IconButtonDefaults.filledIconButtonColors(
					containerColor = MaterialTheme.colorScheme.primary
				)
			) {
				AnimatedContent(
					isPlaying,
					label = "PlayPause",
					transitionSpec = {
						scaleIn(animationSpec = tween(200)) togetherWith scaleOut(animationSpec = tween(200))
					}
				) { playing ->
					Icon(
						if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
						contentDescription = if (playing) "Pausa" else "Play",
						Modifier.size(36.dp)
					)
				}
			}

			// Pulsante successivo con feedback visivo
			FilledTonalIconButton(
				onClick = { transportControls.skipToNext() },
				modifier = Modifier.size(56.dp),
				colors = IconButtonDefaults.filledTonalIconButtonColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
				)
			) {
				Icon(
					Icons.Default.SkipNext,
					contentDescription = "Traccia successiva",
					Modifier.size(28.dp)
				)
			}
		}
	}

	@Composable
	override fun LeftOpenedComposable() {
		val mediaCallback = activeCallback ?: return
		val cover by remember { derivedStateOf { mediaCallback.mediaStruct.cover } }

		Box(
			modifier = Modifier.fillMaxSize().padding(4.dp),
			contentAlignment = Alignment.Center
		) {
			AnimatedContent(
				targetState = cover.value,
				label = "PeekCoverArt",
				transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }
			) { art ->
				if (art != null) {
					Image(
						bitmap = art.asImageBitmap(),
						contentDescription = "Album Art",
						modifier = Modifier.fillMaxSize().clip(CircleShape),
						contentScale = ContentScale.Crop
					)
				} else {
					Box(
						modifier = Modifier.fillMaxSize().clip(CircleShape)
							.background(MaterialTheme.colorScheme.surfaceVariant),
						contentAlignment = Alignment.Center
					) {
						Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
					}
				}
			}
		}
	}

	@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
	@Composable
	override fun RightOpenedComposable() {
		val mediaCallback = activeCallback ?: return
		val title by remember { derivedStateOf { mediaCallback.mediaStruct.title } }
		val artist by remember { derivedStateOf { mediaCallback.mediaStruct.artist } }

		Column(
			modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = title.value,
				style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 1,
				overflow = TextOverflow.Clip,
				modifier = Modifier.basicMarquee()
			)
			Text(
				text = artist.value,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Clip,
				modifier = Modifier.basicMarquee()
			)
		}
	}

	@Composable
	override fun PermissionsRequired() {}
	override fun onLeftSwipe() { activeCallback?.mediaController?.transportControls?.skipToPrevious() }
	override fun onRightSwipe() { activeCallback?.mediaController?.transportControls?.skipToNext() }
}