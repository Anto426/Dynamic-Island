package com.anto426.dynamicisland.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf

object Island {
	var isScreenOn by mutableStateOf(true)
	var isInLandscape by mutableStateOf(false)
	var isLocked by mutableStateOf(false)
	var isIdleHidden by mutableStateOf(false)

	// Stato derivato per ottimizzare le performance
	val isVisible by derivedStateOf {
		isScreenOn && !isLocked && (!isInLandscape || IslandSettings.instance.showInLandscape)
	}

	val shouldShowOnLockScreen by derivedStateOf {
		isScreenOn && isLocked && IslandSettings.instance.showOnLockScreen
	}
}