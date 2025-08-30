package com.anto426.dynamicisland

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.anto426.dynamicisland.model.DISCLOSURE_ACCEPTED
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.ui.theme.DynamicIslandTheme
import com.anto426.dynamicisland.ui.theme.Theme
import com.anto426.dynamicisland.ui.onboarding.OnboardingFlowScreen
import androidx.core.content.edit

class DisclosureActivity : ComponentActivity() {

	private lateinit var settingsPreferences: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Theme.instance.Init()
			WindowCompat.setDecorFitsSystemWindows(window, false)

			settingsPreferences = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)

			DynamicIslandTheme(
				darkTheme = Theme.instance.isDarkTheme,
			) {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background
				) {
					OnboardingFlowScreen(
						onComplete = {
							settingsPreferences.edit { putBoolean(DISCLOSURE_ACCEPTED, true) }
							startActivity(Intent(this@DisclosureActivity, MainActivity::class.java))
							finish()
						}
					)
				}
			}
		}
	}
}