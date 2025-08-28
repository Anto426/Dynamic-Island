package com.anto426.dynamicisland.plugins.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Timeout per la rimozione automatica in millisecondi (es. 2 minuti)
private const val AUTO_HIDE_TIMEOUT = 120_000L

class MediaCallback(
	val mediaController: MediaController,
	private val plugin: MediaSessionPlugin,
	// NUOVO: Callback per notificare il plugin principale di un cambiamento
	private val onStateChanged: () -> Unit
) : MediaController.Callback() {

	val mediaStruct = MediaStruct()
	// NUOVO: Job per la coroutine di auto-rimozione
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
	fun startAutoHideJob() {
		// Se è già in esecuzione, non fare nulla
		if (autoHideJob?.isActive == true) return

		autoHideJob = plugin.pluginScope.launch {
			delay(AUTO_HIDE_TIMEOUT)
			// Dopo il timeout, ricontrolla lo stato
			plugin.updateActiveMediaSession()
		}
	}

	override fun onMetadataChanged(metadata: MediaMetadata?) {
		super.onMetadataChanged(metadata)
		if (metadata == null) return

		mediaStruct.title.value = (metadata.getText(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title").toString()
		mediaStruct.artist.value = (metadata.getText(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist").toString()
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