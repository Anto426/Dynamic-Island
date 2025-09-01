package com.anto426.dynamicisland.plugins.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Autohide removed: keep media visible while relevant

class MediaCallback(
	val mediaController: MediaController,
	private val plugin: MediaSessionPlugin,
	// NUOVO: Callback per notificare il plugin principale di un cambiamento
	private val onStateChanged: () -> Unit
) : MediaController.Callback() {

	val mediaStruct = MediaStruct()
	// Autohide removed: keep reference for compatibility
	private var autoHideJob: Job? = null

	// NUOVO: Metodo per l'aggiornamento iniziale
	fun initialUpdate() {
		onMetadataChanged(mediaController.metadata)
		onPlaybackStateChanged(mediaController.playbackState)
	}

	override fun onPlaybackStateChanged(state: PlaybackState?) {
		super.onPlaybackStateChanged(state)
		if (state == null) return

		mediaStruct.playbackState.value = state

		// Se la musica è in riproduzione, cancella ogni job di auto-rimozione
		if (mediaStruct.isPlaying()) {
			autoHideJob?.cancel()
			autoHideJob = null
		}

		// Notifica il plugin che lo stato è cambiato, così può decidere chi è "attivo"
		onStateChanged()
	}

	// NUOVO: Funzione chiamata dal plugin per avviare il timer di rimozione
	@Suppress("UNUSED_PARAMETER")
	fun startAutoHideJob(timeoutMs: Long = 0L) {
		// No-op: autohide removed
		autoHideJob?.cancel()
		autoHideJob = null
	}

	fun cancelAutoHideJob() {
		autoHideJob?.cancel()
		autoHideJob = null
	}

	override fun onMetadataChanged(metadata: MediaMetadata?) {
		super.onMetadataChanged(metadata)
		if (metadata == null) return

		val titleText = metadata.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()?.trim()
		val artistText = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()?.trim()
		// Non impostare placeholder rumorosi; mantieni stringhe vuote quando assenti
		mediaStruct.title.value = titleText ?: ""
		mediaStruct.artist.value = artistText ?: ""
		mediaStruct.cover.value = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
			?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
					?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
		mediaStruct.duration.value = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

		onStateChanged()
	}

	override fun onSessionDestroyed() {
		super.onSessionDestroyed()
	autoHideJob?.cancel()
	plugin.updateActiveMediaSession() // Aggiorna per rimuovere la sessione distrutta
	}
}