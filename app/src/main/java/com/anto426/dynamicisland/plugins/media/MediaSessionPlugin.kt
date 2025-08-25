package com.anto426.dynamicisland.plugins.media

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.skydoves.landscapist.rememberDrawablePainter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

private object MediaPluginDefaults {
	const val TAG = "MediaSessionPlugin"
	val PlayerArtworkShape = RoundedCornerShape(24.dp)
	val BackgroundBlurRadius = 32.dp
}

class MediaSessionPlugin(
	override val id: String = "MediaSessionPlugin",
	override val name: String = "MediaSession",
	override val description: String = "Show the current media session playing",
	override val permissions: ArrayList<String> = arrayListOf(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override var pluginSettings: MutableMap<String, PluginSettingsItem> = mutableMapOf(),
) : BasePlugin() {

	lateinit var context: IslandOverlayService
	private lateinit var mediaSessionManager: MediaSessionManager
	private val callbackMap = mutableStateMapOf<String, MediaCallback>()

	private val listenerForActiveSessions = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
		controllers?.forEach { registerController(it) }
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
		val componentName = ComponentName(context, NotificationService::class.java)
		try {
			mediaSessionManager.addOnActiveSessionsChangedListener(listenerForActiveSessions, componentName)
			mediaSessionManager.getActiveSessions(componentName).forEach(::registerController)
		} catch (e: SecurityException) {
			Log.e(MediaPluginDefaults.TAG, "Notification Listener permission not granted.", e)
			this.context.removePlugin(this)
		}
	}

	private fun registerController(controller: MediaController) {
		if (callbackMap.containsKey(controller.packageName)) return
		val callback = MediaCallback(controller, this)
		callbackMap[controller.packageName] = callback
		controller.registerCallback(callback)
	}

	fun removeMedia(mediaController: MediaController) {
		callbackMap.remove(mediaController.packageName)
		if (callbackMap.isEmpty()) context.removePlugin(this)
	}

	override fun onClick() {
		callbackMap.values.firstOrNull()?.mediaController?.sessionActivity?.send(0)
	}

	override fun onDestroy() {
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
		val mediaCallback = callbackMap.values.firstOrNull() ?: return
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
			Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
		} else {
			Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
		}
	}

	@Composable
	private fun PlayerArtwork(modifier: Modifier = Modifier, cover: Bitmap?) {
		Card(modifier = modifier, shape = MediaPluginDefaults.PlayerArtworkShape) {
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
						Icon(Icons.Default.MusicNote, "No Cover", modifier = Modifier.size(64.dp))
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
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			AnimatedContent(targetState = title, label = "TitleAnimation") { text ->
				Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.basicMarquee())
			}
			Spacer(modifier = Modifier.height(4.dp))
			AnimatedContent(targetState = artist, label = "ArtistAnimation") { text ->
				Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.basicMarquee())
			}
		}
	}

	@Composable
	private fun PlayerScrubber(mediaStruct: MediaStruct, transportControls: MediaController.TransportControls) {
		var sliderPosition by remember { mutableStateOf(0f) }
		var isDragging by remember { mutableStateOf(false) }
		val duration by remember { mediaStruct.duration }
		val elapsed = mediaStruct.playbackState.value.position

		LaunchedEffect(elapsed, duration) {
			if (!isDragging && duration > 0) { sliderPosition = elapsed.toFloat() / duration }
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Slider(
				value = sliderPosition,
				onValueChange = { isDragging = true; sliderPosition = it },
				onValueChangeFinished = {
					transportControls.seekTo((sliderPosition * duration).roundToLong())
					isDragging = false
				},
				modifier = Modifier.fillMaxWidth()
			)
			Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), Arrangement.SpaceBetween) {
				val current = if (isDragging) (sliderPosition * duration).toLong() else elapsed
				Text(formatTime(current), style = MaterialTheme.typography.labelSmall)
				Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
			}
		}
	}

	@Composable
	private fun PlayerControls(isPlaying: Boolean, transportControls: MediaController.TransportControls) {
		Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
			FilledTonalIconButton({ transportControls.skipToPrevious() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(28.dp)) }
			FilledIconButton({ if (isPlaying) transportControls.pause() else transportControls.play() }, Modifier.size(72.dp)) {
				AnimatedContent(isPlaying, label = "PlayPause") { playing ->
					Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(42.dp))
				}
			}
			FilledTonalIconButton({ transportControls.skipToNext() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, null, Modifier.size(28.dp)) }
		}
	}

	@Composable
	private fun rememberAppIcon(packageName: String) = remember(packageName) {
		try {
			context.packageManager.getApplicationIcon(packageName)
		} catch (e: PackageManager.NameNotFoundException) {
			Log.e(MediaPluginDefaults.TAG, "Icon not found for $packageName", e)
			null
		}
	}

	// MODIFICATO: Ora mostra l'icona dell'app musicale in uso a sinistra.
	@Composable
	override fun LeftOpenedComposable() {
		val mediaCallback = callbackMap.values.firstOrNull() ?: return
		val icon = rememberAppIcon(mediaCallback.mediaController.packageName)

		if (icon != null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(6.dp) // Leggero padding
					.clip(CircleShape),
				contentAlignment = Alignment.Center
			) {
				Image(
					painter = rememberDrawablePainter(drawable = icon),
					contentDescription = "App Icon",
					modifier = Modifier.size(28.dp) // Aumentata la dimensione dell'icona
				)
			}
		}
	}

	// MODIFICATO: Ora mostra una nota musicale che pulsa a destra quando la musica è in riproduzione.
	@Composable
	override fun RightOpenedComposable() {
		val mediaCallback = callbackMap.values.firstOrNull() ?: return
		val isPlaying by remember { derivedStateOf { mediaCallback.mediaStruct.isPlaying() } }

		val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
		val scale by infiniteTransition.animateFloat(
			initialValue = if (isPlaying) 1f else 1.2f,
			targetValue = if (isPlaying) 1.2f else 1f,
			animationSpec = infiniteRepeatable(
				animation = tween(800, easing = FastOutSlowInEasing),
				repeatMode = RepeatMode.Reverse
			),
			label = "PulseScale"
		)

		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier.fillMaxSize()
		) {
			Icon(
				imageVector = Icons.Rounded.MusicNote,
				contentDescription = "Playing music",
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(28.dp).scale(scale) // Dimensione uguale all'icona dell'app
			)
		}
	}

	@Composable
	override fun PermissionsRequired() {}
	override fun onLeftSwipe() { callbackMap.values.firstOrNull()?.mediaController?.transportControls?.skipToPrevious() }
	override fun onRightSwipe() { callbackMap.values.firstOrNull()?.mediaController?.transportControls?.skipToNext() }
}