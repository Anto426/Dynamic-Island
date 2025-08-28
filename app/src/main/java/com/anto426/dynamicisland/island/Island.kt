package com.anto426.dynamicisland.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf

object Island {
	var isScreenOn by mutableStateOf(true)
	var isInLandscape by mutableStateOf(false)

	// Stato derivato per ottimizzare le performance
	val isVisible by derivedStateOf {
		isScreenOn && (!isInLandscape || IslandSettings.instance.showInLandscape)
	}

	val shouldShowOnLockScreen by derivedStateOf {
		isScreenOn && IslandSettings.instance.showOnLockScreen
	}
}