package com.anto426.dynamicisland.plugins.media

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginPriority
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.anto426.dynamicisland.ui.island.PluginDefaults
import com.anto426.dynamicisland.ui.island.SectionCard
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import com.anto426.dynamicisland.R

private object MediaPluginDefaults {
	const val TAG = "MediaSessionPlugin"
	val PlayerArtworkShape = RoundedCornerShape(24.dp)
	val BackgroundBlurRadius = 32.dp
}

class MediaSessionPlugin(
	override val author: String = "Anto426",
	override val description: String = "Show the current media session playing", // Not shown, use descriptionRes
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override val id: String = "MediaSessionPlugin",
	override val name: String = "MediaSession",  
	override val permissions: ArrayList<String> = arrayListOf(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
	override var pluginSettings: SnapshotStateMap<String, PluginSettingsItem> = mutableStateMapOf(),
	override val version: String = "1.0.0",
	override val sourceCodeUrl:String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/media/MediaSessionPlugin.kt"

) : BasePlugin() {
	override val nameRes: Int? get() = R.string.plugin_media_name
	override val descriptionRes: Int? get() = R.string.plugin_media_description

	lateinit var context: IslandOverlayService
	private lateinit var mediaSessionManager: MediaSessionManager
	private val callbackMap = mutableStateMapOf<String, MediaCallback>()

	val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

	private var activeCallback by mutableStateOf<MediaCallback?>(null)
	// Traccia quando abbiamo mostrato per l'ultima volta una sessione in pausa, per evitare loop
	private var lastPausedShownAt: Long = 0L

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
		// Filtra sessioni con contenuti significativi per evitare falsi positivi
		val meaningful = callbackMap.values.filter { it.mediaStruct.hasMeaningfulContent() }
		val playingSession = meaningful.firstOrNull { it.mediaStruct.isPlaying() }

		if (playingSession != null) {
			activeCallback = playingSession
			// Media stays at HIGH to be under notifications
			priority.value = PluginPriority.HIGH
			this.show(context)
			// Reset gestione pausa
			lastPausedShownAt = 0L
			// Autohide removed for media: do not schedule or cancel timers
			playingSession.cancelAutoHideJob()
			return
		}

		// Paused or stopped
		val pausedSession = meaningful.firstOrNull {
			it.mediaStruct.playbackState.value?.state == PlaybackState.STATE_PAUSED
		}
	if (pausedSession != null) {
			activeCallback = pausedSession
			val now = System.currentTimeMillis()
			val shouldShowBriefly = now - lastPausedShownAt > 5_000
			if (shouldShowBriefly) {
				lastPausedShownAt = now
		priority.value = PluginPriority.MEDIUM
		this.show(context, timeoutMs = 0)
			} else {
				this.hide(context)
			}
		} else {
			activeCallback = null
			this.hide(context)
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
		if (millis < 0) return context.getString(R.string.media_time_zero)
		val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
		val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
		return String.format(Locale.getDefault(), context.getString(R.string.media_time_format), minutes, seconds)
	}

	@Composable
	override fun Composable() {
		val mediaCallback = activeCallback ?: return
		val mediaStruct by remember { derivedStateOf { mediaCallback.mediaStruct } }
        val appLabel = remember(mediaCallback) { getAppNameSafe(mediaCallback.mediaController.packageName) }
	val transportControls = remember(mediaCallback) { mediaCallback.mediaController.transportControls }
	val state by remember { mediaStruct.playbackState }
	val isPlaying by remember { derivedStateOf { mediaStruct.isPlaying() } }

		Box(modifier = Modifier.fillMaxSize()) {
			PlayerBackground(cover = mediaStruct.cover.value)
			BoxWithConstraints(
				modifier = Modifier
					.fillMaxSize()
					.padding(PluginDefaults.ContentPadding)
			) {
				val boxMaxWidth = this.maxWidth
				val boxMaxHeight = this.maxHeight
                boxMaxWidth >= 520.dp
				val isCompact = boxMaxWidth < 360.dp
				val isShort = boxMaxHeight < 240.dp
				val isUltraShort = boxMaxHeight < 180.dp

				val sideBtn = when {
					isUltraShort || isCompact -> 40.dp
					isShort -> 44.dp
					else -> 52.dp
				}
				val mainBtn = when {
					isUltraShort || isCompact -> 56.dp
					isShort -> 68.dp
					else -> 78.dp
				}
				val iconSize = when {
					isUltraShort || isCompact -> 24.dp
					isShort -> 28.dp
					else -> 32.dp
				}
				val vSpace = when {
					isUltraShort -> 6.dp
					isShort -> 8.dp
					else -> 10.dp
				}

				// Always use a centered column with a max content width
				val targetMaxWidth = when {
					boxMaxWidth >= 600.dp -> 420.dp
					boxMaxWidth >= 500.dp -> 380.dp
					else -> boxMaxWidth * 0.9f
				}
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Column(
						modifier = Modifier.widthIn(max = targetMaxWidth),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(vSpace)
					) {
						val widthFactor = when {
							isCompact -> 0.62f
							isShort -> 0.74f
							else -> 0.82f
						}
						val artWidth = targetMaxWidth * widthFactor
						val artHeightCap = boxMaxHeight * (if (isUltraShort) 0.28f else if (isShort) 0.36f else 0.46f)
						val absoluteMax = when {
							isUltraShort -> 112.dp
							isShort -> 128.dp
							else -> 144.dp
						}
						val artCandidate = if (artWidth < artHeightCap) artWidth else artHeightCap
						val artSize = if (artCandidate < absoluteMax) artCandidate else absoluteMax
						PlayerArtwork(
							modifier = Modifier.size(artSize),
							cover = mediaStruct.cover.value
						)
						AssistChip(
							onClick = { mediaCallback.mediaController.sessionActivity?.send(0) },
							label = { Text(appLabel, style = MaterialTheme.typography.labelMedium) },
							leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
							colors = AssistChipDefaults.assistChipColors(
								containerColor = MaterialTheme.colorScheme.surfaceContainer,
								labelColor = MaterialTheme.colorScheme.onSurface,
								leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
							)
						)
						TrackDetails(title = mediaStruct.title.value, artist = mediaStruct.artist.value, compact = true)
						SectionCard(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent) {
							PlayerScrubber(mediaStruct = mediaStruct, transportControls = transportControls, showLabels = !isUltraShort)
							Spacer(Modifier.height(4.dp))
							PlayerControls(isPlaying = isPlaying, actions = state?.actions ?: 0L, transportControls = transportControls, sideButtonSize = sideBtn, mainButtonSize = mainBtn, centerIconSize = iconSize)
						}
					}
				}
			}
		}
	}

	private fun getAppNameSafe(packageName: String): String = runCatching {
		val pm = context.packageManager
		val ai = pm.getApplicationInfo(packageName, 0)
		pm.getApplicationLabel(ai).toString()
	}.getOrElse { packageName }

	@Composable
	private fun PlayerBackground(cover: Bitmap?) {
		if (cover != null) {
			Image(
				bitmap = cover.asImageBitmap(),
				contentDescription = stringResource(R.string.media_blurred_background_desc),
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
						contentDescription = stringResource(R.string.media_album_cover_desc),
						modifier = Modifier.fillMaxSize(),
						contentScale = ContentScale.Crop
					)
				} else {
					Box(
						contentAlignment = Alignment.Center,
						modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
					) {
			Icon(Icons.Default.MusicNote, stringResource(R.string.media_no_cover), modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
					}
				}
			}
		}
	}

	@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
	@Composable
	private fun TrackDetails(modifier: Modifier = Modifier, title: String, artist: String, compact: Boolean = false) {
		Column(
			modifier = modifier.fillMaxWidth(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			AnimatedContent(targetState = title, label = "TitleAnimation", transitionSpec = {
				slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
			}) { text ->
				val titleStyle = if (compact) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
				Text(
					text,
					style = titleStyle,
					maxLines = 1,
					modifier = Modifier.fillMaxWidth().basicMarquee(),
					textAlign = androidx.compose.ui.text.style.TextAlign.Center
				)
			}
			Spacer(modifier = Modifier.height(4.dp))
			AnimatedContent(targetState = artist, label = "ArtistAnimation", transitionSpec = {
				slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
			}) { text ->
				val artistStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
				Text(
					text,
					style = artistStyle,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					modifier = Modifier.fillMaxWidth().basicMarquee(),
					textAlign = androidx.compose.ui.text.style.TextAlign.Center
				)
			}
		}
	}

	@Composable
	private fun PlayerScrubber(mediaStruct: MediaStruct, transportControls: MediaController.TransportControls, showLabels: Boolean = true) {
		var sliderPosition by remember { mutableFloatStateOf(0f) }
		var isDragging by remember { mutableStateOf(false) }
		val duration by remember { mediaStruct.duration }
		val playbackState by remember { mediaStruct.playbackState }
		var elapsed by remember { mutableLongStateOf(0L) }

		fun calcEffective(state: PlaybackState?, duration: Long): Long {
			if (state == null) return 0L
			var pos = state.position
			if (state.state == PlaybackState.STATE_PLAYING) {
				val delta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
				pos += (delta * state.playbackSpeed).toLong()
			}
			return if (duration > 0) pos.coerceIn(0L, duration) else maxOf(0L, pos)
		}

		LaunchedEffect(playbackState, duration) {
			// ticker updating elapsed when playing
			while (true) {
				elapsed = calcEffective(playbackState, duration)
				if (!isDragging) {
					sliderPosition = if (duration > 0) elapsed.toFloat() / duration else 0f
				}
				delay(250)
			}
		}

		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			if (duration > 0L) {
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
			} else {
				AssistChip(
					onClick = {},
					label = { Text("Live") },
					leadingIcon = { Icon(Icons.Default.WifiTethering, contentDescription = null) },
					colors = AssistChipDefaults.assistChipColors(
						containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
						labelColor = MaterialTheme.colorScheme.onSurface,
						leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
					)
				)
			}
			if (showLabels) {
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
						if (duration > 0L) formatTime(duration) else "—",
						style = MaterialTheme.typography.labelSmall.copy(
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					)
				}
			}
		}
	}

	@Composable
	private fun PlayerControls(
		isPlaying: Boolean,
		actions: Long,
		transportControls: MediaController.TransportControls,
		sideButtonSize: Dp,
		mainButtonSize: Dp,
		centerIconSize: Dp
	) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 4.dp),
			Arrangement.SpaceEvenly,
			Alignment.CenterVertically
		) {
			val canPrev = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
			val canNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L
			// Pulsante precedente con feedback visivo
			FilledTonalIconButton(
				onClick = { if (canPrev) transportControls.skipToPrevious() },
				modifier = Modifier.size(sideButtonSize),
				colors = IconButtonDefaults.filledTonalIconButtonColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
				)
			, enabled = canPrev) {
					Icon(
						Icons.Default.SkipPrevious,
						contentDescription = stringResource(R.string.media_previous_track),
					Modifier.size(centerIconSize * 0.78f)
				)
			}

			// Pulsante play/pause principale
			FilledIconButton(
				onClick = { if (isPlaying) transportControls.pause() else transportControls.play() },
				modifier = Modifier.size(mainButtonSize),
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
						contentDescription = if (playing) stringResource(R.string.media_pause) else stringResource(R.string.media_play),
						Modifier.size(centerIconSize)
					)
				}
			}

			// Pulsante successivo con feedback visivo
			FilledTonalIconButton(
				onClick = { if (canNext) transportControls.skipToNext() },
				modifier = Modifier.size(sideButtonSize),
				colors = IconButtonDefaults.filledTonalIconButtonColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
				)
			, enabled = canNext) {
					Icon(
						Icons.Default.SkipNext,
						contentDescription = stringResource(R.string.media_next_track),
					Modifier.size(centerIconSize * 0.78f)
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
						contentDescription = stringResource(R.string.media_album_art_desc),
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