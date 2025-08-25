package com.anto426.dynamicisland.island

import android.content.res.Configuration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface IslandState {
	val yPosition: Dp
		get() = IslandSettings.instance.positionY.dp
	val xPosition: Dp
		get() = IslandSettings.instance.positionX.dp
	val height: Dp
	val width: Dp
	val cornerPercentage: Float
	val state: IslandStates
}

sealed class IslandViewState : IslandState {

	object Closed : IslandViewState() {
		override val height: Dp = 34.dp
		override val width: Dp = 34.dp
		override val cornerPercentage: Float = 100f
		override val state: IslandStates = IslandStates.Closed
	}

	object Opened : IslandViewState() {
		override val height: Dp = 34.dp
		override val width: Dp
			get() = IslandSettings.instance.width.dp
		override val cornerPercentage: Float = 100f
		override val state: IslandStates = IslandStates.Opened
	}

	// 'configuration' exists ONLY inside this class
	class Expanded(private val configuration: Configuration) : IslandViewState() {
		override val height: Dp
			get() = (configuration.screenHeightDp.dp * 0.5f)

		override val width: Dp
			get() = configuration.screenWidthDp.dp - xPosition * 2

		override val cornerPercentage: Float
			get() = IslandSettings.instance.cornerRadius.toFloat()

		override val state: IslandStates = IslandStates.Expanded
	}
}

enum class IslandStates {
	Closed,
	Opened,
	Expanded
}