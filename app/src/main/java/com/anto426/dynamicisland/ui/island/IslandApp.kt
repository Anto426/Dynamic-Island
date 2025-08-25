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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
		animationSpec = spring(
			dampingRatio = DampingRatioMediumBouncy,
			stiffness = StiffnessLow
		),
		label = "IslandHeight"
	)
	val width by animateDpAsState(
		targetValue = islandView.width,
		animationSpec = spring(
			dampingRatio = DampingRatioMediumBouncy,
			stiffness = StiffnessLow
		),
		label = "IslandWidth"
	)
	val cornerPercentage by animateFloatAsState(
		targetValue = islandView.cornerPercentage,
		label = "IslandCorner"
	)

	AnimatedVisibility(
		visible = (Island.isScreenOn || IslandSettings.instance.showOnLockScreen)
				&& (!Island.isInLandscape || IslandSettings.instance.showInLandscape),
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
						.then(
							if (islandView is IslandViewState.Opened || islandView is IslandViewState.Expanded) {
								Modifier.combinedClickable(
									onClick = { bindedPlugin?.onClick() },
									onLongClick = {
										if (bindedPlugin?.canExpand() == true) {
											islandOverlayService.expand()
										}
									}
								)
							} else Modifier
						)
						.then(
							if (IslandSettings.instance.showBorders) {
								Modifier.border(
									width = 1.dp,
									color = MaterialTheme.colorScheme.primary,
									shape = RoundedCornerShape(cornerPercentage)
								)
							} else Modifier
						),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surface,
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

		Box(
			modifier = contentModifier,
			contentAlignment = Alignment.Center
		) {
			Crossfade(targetState = leftContent, animationSpec = tween(300), label = "LeftPluginCrossfade") { it() }
		}
		Box(
			modifier = contentModifier,
			contentAlignment = Alignment.Center
		) {
			Crossfade(targetState = rightContent, animationSpec = tween(300), label = "RightPluginCrossfade") { it() }
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
		Box(modifier = Modifier.weight(1f)) {
			Crossfade(targetState = content, animationSpec = tween(300), label = "ExpandedPluginCrossfade") { it() }
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
			.clickable(onClick = onClick)
			.padding(vertical = 12.dp),
		contentAlignment = Alignment.Center
	) {
		Box(
			modifier = Modifier
				.width(40.dp)
				.height(4.dp)
				.clip(CircleShape)
				.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
		)
	}
}