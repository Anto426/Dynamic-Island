package com.anto426.dynamicisland.plugins.media

import android.graphics.Bitmap
import android.media.session.PlaybackState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MediaStruct(
	var artist: MutableState<String> = mutableStateOf(""),
	var title: MutableState<String> = mutableStateOf(""),
	var cover: MutableState<Bitmap?> = mutableStateOf(null),
	var playbackState: MutableState<PlaybackState?> = mutableStateOf(null), // Può essere null inizialmente
	var duration: MutableState<Long> = mutableStateOf(0L),
) {
	fun isPlaying(): Boolean {
		// Aggiunto controllo null-safety
		return playbackState.value?.state == PlaybackState.STATE_PLAYING
	}
}