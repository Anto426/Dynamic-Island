package com.anto426.dynamicisland.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.STYLE
import com.anto426.dynamicisland.model.THEME
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.ui.theme.themes.BlackTheme
import com.anto426.dynamicisland.ui.theme.themes.QuinacridoneMagentaThemeDarkColors
import com.anto426.dynamicisland.ui.theme.themes.QuinacridoneMagentaThemeLightColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

class Theme {

	enum class ThemeStyle(
		val lightScheme: ColorScheme? = null,
		val darkScheme: ColorScheme? = null,
		val styleName: String,
		val previewColorLight: Color?,
		val previewColorDark: Color?
	) {
		MaterialYou(
			styleName = "Material You",
			previewColorLight = null,
			previewColorDark = null
		),
		Black(
			darkScheme = BlackTheme,
			styleName = "Black & White",
			previewColorLight = Color.Black,
			previewColorDark = Color.White
		),
		QuinacridoneMagenta(
			lightScheme = QuinacridoneMagentaThemeLightColors,
			darkScheme = QuinacridoneMagentaThemeDarkColors,
			styleName = "Quinacridone Magenta",
			previewColorLight = QuinacridoneMagentaThemeLightColors.primary,
			previewColorDark = QuinacridoneMagentaThemeDarkColors.primary
		),
	}

	companion object {
		val instance = Theme()
	}

	var isDarkTheme by mutableStateOf(false)
	var themeStyle by mutableStateOf(ThemeStyle.MaterialYou)

	@Composable
	fun Init(
		isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
	) {
		val context = LocalContext.current
		val settingsPreferences = context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)

		isDarkTheme = when (settingsPreferences.getString(THEME, "System")) {
			"System" -> { isSystemInDarkTheme }
			"Dark" -> { true }
			"Light" -> { false }
			else -> { isSystemInDarkTheme }
		}

		themeStyle = when (settingsPreferences.getString(STYLE, "MaterialYou")) {
			ThemeStyle.MaterialYou.name -> { ThemeStyle.MaterialYou }
			ThemeStyle.Black.name -> { ThemeStyle.Black }
			ThemeStyle.QuinacridoneMagenta.name -> { ThemeStyle.QuinacridoneMagenta }
			else -> { ThemeStyle.MaterialYou }
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DynamicIslandTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	style: Theme.ThemeStyle = Theme.instance.themeStyle,
	content: @Composable () -> Unit
) {
	val context = LocalContext.current
	val activity = context as? ComponentActivity
	SideEffect {
		activity?.let { act ->
			// edge-to-edge without deprecated setters
			val scrim = Color.Transparent.toArgb()
			act.enableEdgeToEdge(
				statusBarStyle = if (darkTheme) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim),
				navigationBarStyle = if (darkTheme) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim)
			)
			// Ensure content draws under system bars
			WindowCompat.setDecorFitsSystemWindows(act.window, false)
		}
	}

	MaterialTheme(
		colorScheme = when(style) {
			Theme.ThemeStyle.MaterialYou -> {
				if (IslandSettings.instance.dynamicThemeEnabled) {
					if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
				} else {
					if (darkTheme) darkColorScheme() else lightColorScheme()
				}
			}
			else -> {
				if (darkTheme) {
					style.darkScheme ?: style.lightScheme ?: if (IslandSettings.instance.dynamicThemeEnabled) dynamicDarkColorScheme(context) else darkColorScheme()
				} else {
					style.lightScheme ?: style.darkScheme ?: if (IslandSettings.instance.dynamicThemeEnabled) dynamicLightColorScheme(context) else lightColorScheme()
				}
			}
		},
		typography = Typography,
		content = content
	)
}

/**
 * Dynamic gradient modifier that creates a beautiful Material You themed background
 * Uses multiple layers of primary, secondary, and tertiary colors for depth and richness
 */
@Composable
fun Modifier.dynamicGradient(): Modifier {
	val colorScheme = MaterialTheme.colorScheme
	return this.background(
		brush = Brush.verticalGradient(
			colors = listOf(
				// Top layer - vibrant primary colors
				colorScheme.primary.copy(alpha = 0.12f),
				colorScheme.primaryContainer.copy(alpha = 0.18f),

				// Middle layer - secondary colors for depth
				colorScheme.secondary.copy(alpha = 0.08f),
				colorScheme.secondaryContainer.copy(alpha = 0.15f),

				// Accent layer - tertiary for richness
				colorScheme.tertiary.copy(alpha = 0.06f),
				colorScheme.tertiaryContainer.copy(alpha = 0.12f),

				// Surface transition - smooth blend to background
				colorScheme.surface.copy(alpha = 0.9f),
				colorScheme.surfaceVariant.copy(alpha = 0.95f),

				// Bottom layer - clean background
				colorScheme.background
			)
		)
	)
}

/**
 * Horizontal gradient modifier for cards and accent elements
 * Creates a subtle left-to-right color transition
 */
@Composable
fun Modifier.dynamicHorizontalGradient(): Modifier {
	val colorScheme = MaterialTheme.colorScheme
	return this.background(
		brush = Brush.horizontalGradient(
			colors = listOf(
				colorScheme.primary.copy(alpha = 0.1f),
				colorScheme.secondary.copy(alpha = 0.08f),
				colorScheme.tertiary.copy(alpha = 0.06f),
				colorScheme.surfaceVariant.copy(alpha = 0.9f),
				colorScheme.background
			)
		)
	)
}