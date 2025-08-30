package com.anto426.dynamicisland.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.STYLE
import com.anto426.dynamicisland.model.THEME
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
	// Dynamic color is available on Android 12+
	content: @Composable () -> Unit
) {
	/**
	 * Enhanced Material You Theme with:
	 * - Dynamic color scheme based on system wallpaper
	 * - Improved typography with proper Material 3 scaling
	 * - Rich gradient backgrounds using primary/secondary/tertiary colors
	 * - Transparent system bars with adaptive icon colors
	 * - Support for multiple theme styles (Material You, Black, Quinacridone Magenta)
	 */

	val context = LocalContext.current
	val systemUiController = rememberSystemUiController()

	SideEffect {
		systemUiController.setSystemBarsColor(
			color = Color.Transparent,
			darkIcons = when(style) {
				Theme.ThemeStyle.MaterialYou -> {
					!darkTheme
				}
				Theme.ThemeStyle.Black -> {
					false // White icons on black
				}
				Theme.ThemeStyle.QuinacridoneMagenta -> {
					!darkTheme
				}
			}
		)
		systemUiController.setNavigationBarColor(
			color = Color.Transparent,
			darkIcons = when(style) {
				Theme.ThemeStyle.MaterialYou -> {
					!darkTheme
				}
				Theme.ThemeStyle.Black -> {
					false
				}
				Theme.ThemeStyle.QuinacridoneMagenta -> {
					!darkTheme
				}
			}
		)
	}

	MaterialTheme(
		colorScheme = when(style) {
			Theme.ThemeStyle.MaterialYou -> {
				if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
			}
			else -> {
				if (darkTheme) {
					style.darkScheme ?: style.lightScheme ?: dynamicDarkColorScheme(context)
				} else {
					style.lightScheme ?: style.darkScheme ?: dynamicLightColorScheme(context)
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