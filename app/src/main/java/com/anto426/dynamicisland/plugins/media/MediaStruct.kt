package com.anto426.dynamicisland.plugins.media

import android.graphics.Bitmap
import android.media.session.PlaybackState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

class MediaStruct(
    var artist: MutableState<String> = mutableStateOf(""),
    var title: MutableState<String> = mutableStateOf(""),
    var cover: MutableState<Bitmap?> = mutableStateOf(null),
    var playbackState: MutableState<PlaybackState?> = mutableStateOf(null),
    var duration: MutableState<Long> = mutableLongStateOf(0L),
) {
	fun isPlaying(): Boolean {
		// Aggiunto controllo null-safety
		return playbackState.value?.state == PlaybackState.STATE_PLAYING
	}

	fun hasMeaningfulContent(): Boolean {
		// Evita falsi positivi quando il sistema espone una sessione vuota o stantia
		val hasText = title.value.isNotBlank() || artist.value.isNotBlank()
		val hasArtOrDuration = (cover.value != null) || (duration.value > 0)
		return hasText || hasArtOrDuration
	}
}