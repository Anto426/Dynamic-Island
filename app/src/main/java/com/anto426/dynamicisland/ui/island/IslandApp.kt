package com.anto426.dynamicisland.ui.island

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.island.*
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.ui.theme.DynamicIslandTheme
import com.anto426.dynamicisland.ui.theme.Theme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandApp(
	islandOverlayService: IslandOverlayService
) {
	val context = LocalContext.current
	Theme.instance.Init()
	LaunchedEffect(Unit) {
		IslandSettings.instance.loadSettings(context = context)
	}

	val islandView = islandOverlayService.islandState
	val bindedPlugin = islandOverlayService.bindedPlugins.firstOrNull()

	val height by animateDpAsState(
		targetValue = islandView.height,
		animationSpec = if (islandOverlayService.shouldAnimate()) {
			spring(
				dampingRatio = DampingRatioMediumBouncy,
				stiffness = StiffnessLow
			)
		} else {
			spring(
				dampingRatio = 1.0f,  // No bouncy
				stiffness = 10000f    // High stiffness
			)
		},
		label = "IslandHeight"
	)
	val width by animateDpAsState(
		targetValue = islandView.width,
		animationSpec = if (islandOverlayService.shouldAnimate()) {
			spring(
				dampingRatio = DampingRatioMediumBouncy,
				stiffness = StiffnessLow
			)
		} else {
			spring(
				dampingRatio = 1.0f,  // No bouncy
				stiffness = 10000f    // High stiffness
			)
		},
		label = "IslandWidth"
	)
	val cornerPercentage by animateFloatAsState(
		targetValue = islandView.cornerPercentage,
		animationSpec = if (islandOverlayService.shouldAnimate()) {
			tween(
				durationMillis = 400,
				easing = EaseOutCubic
			)
		} else {
			tween(durationMillis = 0)
		},
		label = "IslandCorner"
	)

	// Aggiungiamo animazione di scala per effetti più dinamici
	val scale by animateFloatAsState(
		targetValue = when (islandView.state) {
			IslandStates.Closed -> 1f
			IslandStates.Opened -> if (islandOverlayService.shouldAnimate()) 1.05f else 1f
			IslandStates.Expanded -> 1f
		},
		animationSpec = if (islandOverlayService.shouldAnimate()) {
			spring(
				dampingRatio = DampingRatioMediumBouncy,
				stiffness = 1500f  // Medium stiffness
			)
		} else {
			spring(
				dampingRatio = 1.0f,  // No bouncy
				stiffness = 10000f    // High stiffness
			)
		},
		label = "IslandScale"
	)

	AnimatedVisibility(
		visible = Island.isVisible || Island.shouldShowOnLockScreen,
	) {
		DynamicIslandTheme(
			darkTheme = if (islandOverlayService.invertedTheme) !Theme.instance.isDarkTheme else Theme.instance.isDarkTheme,
			style = Theme.instance.themeStyle
		) {
			Box(
				modifier = Modifier
					.padding(top = islandView.yPosition)
					.fillMaxWidth()
					.height(height)
					.offset(x = islandView.xPosition),
				contentAlignment = when (IslandSettings.instance.gravity) {
					IslandGravity.Center -> Alignment.TopCenter
					IslandGravity.Left -> Alignment.TopStart
					IslandGravity.Right -> Alignment.TopEnd
				}
			) {
				Card(
					shape = RoundedCornerShape(cornerPercentage),
					modifier = Modifier
						.width(width)
						.height(height)
						.graphicsLayer(
							scaleX = scale,
							scaleY = scale
						)
						.combinedClickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = {
								bindedPlugin?.onClick()
								islandOverlayService.performHapticFeedback()
								islandOverlayService.playSound()
							},
							onLongClick = {
								if (bindedPlugin?.canExpand() == true) {
									islandOverlayService.expand()
									islandOverlayService.performHapticFeedback()
								}
							}
						)
						.let {
							if (IslandSettings.instance.showBorders) {
								it.border(
									width = 2.dp,
									color = MaterialTheme.colorScheme.primary,
									shape = RoundedCornerShape(cornerPercentage)
								)
							} else {
								it
							}
						},
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surface,
					),
					elevation = CardDefaults.cardElevation(
						defaultElevation = when (islandView.state) {
							IslandStates.Closed -> 4.dp
							IslandStates.Opened -> 12.dp
							IslandStates.Expanded -> 16.dp
						},
						pressedElevation = 20.dp
					)
				) {
					Crossfade(
						targetState = islandOverlayService.islandState.state,
						animationSpec = tween(300),
						label = "IslandStateCrossfade"
					) { state ->
						when (state) {
							IslandStates.Opened -> {
								IslandOpenedContent(
									leftContent = { bindedPlugin?.LeftOpenedComposable() },
									rightContent = { bindedPlugin?.RightOpenedComposable() }
								)
							}

							IslandStates.Expanded -> {
								IslandExpandedContent(
									service = islandOverlayService,
									content = { bindedPlugin?.Composable() }
								)
							}

							else -> {}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun IslandOpenedContent(
	leftContent: @Composable () -> Unit,
	rightContent: @Composable () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxSize()
			.padding(4.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		val contentModifier = Modifier
			.fillMaxHeight()
			.weight(1f)

		// Removed unnecessary nested Crossfades
		Box(
			modifier = contentModifier,
			contentAlignment = Alignment.Center
		) {
			leftContent()
		}
		Box(
			modifier = contentModifier,
			contentAlignment = Alignment.Center
		) {
			rightContent()
		}
	}
}

@Composable
private fun IslandExpandedContent(
	service: IslandOverlayService,
	content: @Composable () -> Unit
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier.weight(1f).fillMaxWidth(),
			contentAlignment = Alignment.Center
		) {
			// Removed unnecessary nested Crossfade
			content()
		}
		CloseHandle(
			onClick = { service.shrink() }
		)
	}
}

@Composable
private fun CloseHandle(
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = onClick
			)
			.padding(vertical = 16.dp),
		contentAlignment = Alignment.Center
	) {
		Box(
			modifier = Modifier
				.width(48.dp)
				.height(6.dp)
				.clip(RoundedCornerShape(3.dp))
				.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
		)
	}
}