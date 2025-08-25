package com.anto426.dynamicisland.ui.animation

import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavesLoadingIndicator(modifier: Modifier, color: Color, progress: Float) {
	BoxWithConstraints(
		modifier = modifier.offset(y = 16.dp),
		contentAlignment = Alignment.Center
	) {
		val density = LocalDensity.current
		val widthPx = with(density) { maxWidth.roundToPx() }
		val heightPx = with(density) { maxHeight.roundToPx() }

		// Shader: generato una sola volta per tile piccola, poi ripetuto
		val wavesShader = remember(widthPx, heightPx, color) {
			createWavesShader(
				width = 200, // tile piccola
				height = 200,
				color = color
			)
		}

		if (progress > 0f) {
			WavesOnCanvas(
				shader = wavesShader,
				progress = progress.coerceAtMost(0.99f)
			)
		}
	}
}

@Composable
private fun WavesOnCanvas(shader: Shader, progress: Float) {
	val matrix = remember { Matrix() }
	val paint = remember(shader) {
		Paint().apply {
			isAntiAlias = true
			this.shader = shader
		}
	}

	// Animazioni fluide con Animatable
	val scope = rememberCoroutineScope()
	val waveShift = remember { Animatable(0f) }
	val amplitude = remember { Animatable(0.01f) }

	LaunchedEffect(Unit) {
		scope.launch {
			waveShift.animateTo(
				targetValue = 1f,
				animationSpec = infiniteRepeatable(
					tween(WavesShiftAnimationDurationInMillis, easing = LinearEasing),
					repeatMode = RepeatMode.Restart
				)
			)
		}
		scope.launch {
			amplitude.animateTo(
				targetValue = 0.015f,
				animationSpec = infiniteRepeatable(
					tween(WavesAmplitudeAnimationDurationInMillis, easing = FastOutLinearInEasing),
					repeatMode = RepeatMode.Reverse
				)
			)
		}
	}

	Canvas(modifier = Modifier.fillMaxSize()) {
		val h = size.height
		val w = size.width
		matrix.setScale(
			1f,
			amplitude.value / AmplitudeRatio,
			0f,
			WaterLevelRatio * h
		)
		matrix.postTranslate(
			waveShift.value * w,
			(WaterLevelRatio - progress) * h
		)
		shader.setLocalMatrix(matrix)
		drawIntoCanvas { it.drawRect(0f, 0f, w, h, paint) }
	}
}

@Stable
private fun createWavesShader(width: Int, height: Int, color: Color): Shader {
	val angularFrequency = 2f * PI / width
	val amplitude = height * AmplitudeRatio
	val waterLevel = height * WaterLevelRatio

	val bitmap = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
	val canvas = Canvas(bitmap)
	val wavePaint = Paint().apply {
		strokeWidth = 2f
		isAntiAlias = true
	}

	val waveY = FloatArray(width + 1)

	// Prima onda trasparente
	wavePaint.color = color.copy(alpha = 0.3f)
	for (x in 0..width) {
		val wx = x * angularFrequency
		val y = waterLevel + amplitude * sin(wx).toFloat()
		canvas.drawLine(
			Offset(x.toFloat(), y),
			Offset(x.toFloat(), (height + 1).toFloat()),
			wavePaint
		)
		waveY[x] = y
	}

	// Seconda onda piena
	wavePaint.color = color
	val endX = width + 1
	val waveToShift = width / 4
	for (x in 0..width) {
		canvas.drawLine(
			Offset(x.toFloat(), waveY[(x + waveToShift).rem(endX)]),
			Offset(x.toFloat(), (height + 1).toFloat()),
			wavePaint
		)
	}

	return ImageShader(
		image = bitmap,
		tileModeX = TileMode.Repeated,
		tileModeY = TileMode.Clamp
	)
}

private const val AmplitudeRatio = 0.05f
private const val WaterLevelRatio = 0.5f
private const val WavesShiftAnimationDurationInMillis = 2500
private const val WavesAmplitudeAnimationDurationInMillis = 3000
