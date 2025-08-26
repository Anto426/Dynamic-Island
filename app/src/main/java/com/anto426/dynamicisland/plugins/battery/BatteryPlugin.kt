package com.anto426.dynamicisland.plugins.battery

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.anto426.dynamicisland.model.BATTERY_SHOW_PERCENTAGE
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.anto426.dynamicisland.ui.theme.BatteryEmpty
import com.anto426.dynamicisland.ui.theme.BatteryFull
import com.github.compose.waveloading.DrawType
import com.github.compose.waveloading.WaveLoading
import java.util.concurrent.TimeUnit

private enum class DisplayMode {
	CHARGING, LOW_BATTERY
}

class BatteryShape : Shape {
	override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
		val path = Path()
		val cornerRadius = CornerRadius(size.width * 0.1f, size.width * 0.1f)
		val terminalHeight = size.height * 0.05f
		val terminalWidth = size.width * 0.4f
		val bodyHeight = size.height - terminalHeight

		path.addRoundRect(
			roundRect = RoundRect(rect = Rect(offset = Offset(0f, terminalHeight), size = Size(size.width, bodyHeight)), cornerRadius = cornerRadius)
		)
		path.addRoundRect(
			roundRect = RoundRect(rect = Rect(offset = Offset((size.width - terminalWidth) / 2, 0f), size = Size(terminalWidth, terminalHeight)), cornerRadius = CornerRadius(cornerRadius.x / 2, cornerRadius.y / 2))
		)
		return Outline.Generic(path)
	}
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
		Spacer(modifier = Modifier.width(16.dp))
		Text(text = label, style = MaterialTheme.typography.bodyLarge)
		Spacer(modifier = Modifier.weight(1f))
		Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
	}
}

class BatteryPlugin(
	override val id: String = "BatteryPlugin",
	override val name: String = "Battery",
	override val description: String = "Show the current battery level when charging",
	override val permissions: ArrayList<String> = arrayListOf(),
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override var pluginSettings: MutableMap<String, PluginSettingsItem> = mutableMapOf(
		BATTERY_SHOW_PERCENTAGE to PluginSettingsItem.SwitchSettingsItem(
			title = "Show percentage",
			description = "Show the battery percentage",
			id = BATTERY_SHOW_PERCENTAGE,
			value = mutableStateOf(true),
		),
	),
) : BasePlugin() {

	private lateinit var context: IslandOverlayService
	private lateinit var batteryManager: BatteryManager
	private var batteryPercent by mutableStateOf(0)
	private var isCharging by mutableStateOf(false)
	private var chargeTimeRemaining by mutableStateOf(-1L)
	private var batteryTemperature by mutableStateOf(0f)
	private var batteryHealth by mutableStateOf("")
	private var chargingSource by mutableStateOf("")

	private var displayMode by mutableStateOf<DisplayMode?>(null)
	private val LOW_BATTERY_THRESHOLD = 20

	private val batteryBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
			val newIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
			val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
			val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
			batteryPercent = (level * 100 / scale)
			isCharging = newIsCharging

			val isLow = batteryPercent <= LOW_BATTERY_THRESHOLD && !isCharging

			if (isCharging) {
				chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
				batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
				batteryHealth = mapHealthToString(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0))
				chargingSource = mapPluggedToString(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))

				if (displayMode != DisplayMode.CHARGING) {
					displayMode = DisplayMode.CHARGING
					this@BatteryPlugin.context.addPlugin(this@BatteryPlugin)
				}
			} else if (isLow) {
				if (displayMode != DisplayMode.LOW_BATTERY) {
					displayMode = DisplayMode.LOW_BATTERY
					this@BatteryPlugin.context.addPlugin(this@BatteryPlugin)
				}
			} else {
				if (displayMode != null) {
					displayMode = null
					this@BatteryPlugin.context.removePlugin(this@BatteryPlugin)
				}
			}
		}
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		this.batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
		val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
		context.registerReceiver(batteryBroadcastReceiver, intentFilter)

		val initialStatusIntent: Intent? = context.registerReceiver(null, intentFilter)
		if (initialStatusIntent != null) {
			batteryBroadcastReceiver.onReceive(context, initialStatusIntent)
		}
		pluginSettings.values.forEach {
			if (it is PluginSettingsItem.SwitchSettingsItem) {
				it.value.value = it.isSettingEnabled(context, it.id)
			}
		}
	}

	@Composable
	override fun Composable() {
		when (displayMode) {
			DisplayMode.CHARGING -> ChargingView()
			DisplayMode.LOW_BATTERY -> LowBatteryView()
			null -> {}
		}
	}

	@SuppressLint("Range")
    @Composable
	private fun ChargingView() {
		val animatedProgress = animateFloatAsState(
			targetValue = batteryPercent / 100f,
			animationSpec = tween(1000),
			label = "BatteryProgress"
		).value
		val progressColor = lerp(BatteryEmpty, BatteryFull, animatedProgress)

		Column(
			modifier = Modifier.fillMaxSize().padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.SpaceAround
		) { 			Box(
			modifier = Modifier
				.fillMaxHeight(0.5f)
				.aspectRatio(0.55f),
			contentAlignment = Alignment.Center
		) {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.clip(BatteryShape())
						.background(progressColor.copy(alpha = 0.3f)),
					contentAlignment = Alignment.BottomCenter
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.fillMaxHeight(animatedProgress)
							.background(progressColor)
					)
				}
				Text(
					text = "$batteryPercent%",
					style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		}

			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				InfoRow(Icons.Rounded.HourglassTop, "Tempo rimanente", formatChargeTime(chargeTimeRemaining))
				InfoRow(Icons.Rounded.DeviceThermostat, "Temperatura", "${"%.1f".format(batteryTemperature)}°C")
				InfoRow(Icons.Rounded.Power, "Fonte", chargingSource)
				InfoRow(Icons.Rounded.HealthAndSafety, "Salute", batteryHealth)
			}
		}
	}

	@Composable
	private fun LowBatteryView() {
		Column(
			modifier = Modifier.fillMaxSize().padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.SpaceEvenly
		) {
			Icon(
				imageVector = Icons.Rounded.BatteryAlert,
				contentDescription = "Batteria Scarica",
				modifier = Modifier.size(64.dp),
				tint = BatteryEmpty
			)
			Text(
				text = "Batteria Scarica",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold
			)
			Text(
				text = "$batteryPercent% rimanente",
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceAround
			) {
				TextButton(onClick = { context.removePlugin(this@BatteryPlugin) }) {
					Text("Ignora")
				}
				Button(onClick = {
					val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
					intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
					context.startActivity(intent)
					context.shrink()
				}) {
					Text("Risparmio Energetico")
				}
			}
		}
	}

	@Composable
	override fun LeftOpenedComposable() {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier.fillMaxSize().padding(8.dp)
		) {
			when (displayMode) {
				DisplayMode.CHARGING -> {
					val progressColor = lerp(BatteryEmpty, BatteryFull, batteryPercent / 100f)
					Icon(
						imageVector = Icons.Rounded.Bolt, "Charging",
						tint = progressColor, modifier = Modifier.fillMaxSize()
					)
				}
				DisplayMode.LOW_BATTERY -> {
					Icon(
						imageVector = Icons.Rounded.BatteryAlert, "Low Battery",
						tint = BatteryEmpty, modifier = Modifier.fillMaxSize()
					)
				}
				null -> {}
			}
		}
	}

	@Composable
	override fun RightOpenedComposable() {
		val showPercentage by remember {
			derivedStateOf {
				(pluginSettings[BATTERY_SHOW_PERCENTAGE] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value ?: true
			}
		}
		if (showPercentage) {
			val color = when (displayMode) {
				DisplayMode.CHARGING -> lerp(BatteryEmpty, BatteryFull, batteryPercent / 100f)
				DisplayMode.LOW_BATTERY -> BatteryEmpty
				else -> MaterialTheme.colorScheme.onSurface
			}
			Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = "$batteryPercent%",
					style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
					color = color
				)
			}
		}
	}

	override fun onDestroy() {
		if (!::context.isInitialized) return
		try {
			context.unregisterReceiver(batteryBroadcastReceiver)
		} catch (e: IllegalArgumentException) {}
	}

	private fun mapHealthToString(health: Int): String {
		return when (health) {
			BatteryManager.BATTERY_HEALTH_GOOD -> "Buona"
			BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surriscaldata"
			BatteryManager.BATTERY_HEALTH_DEAD -> "Esausta"
			BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sovratensione"
			BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Guasto"
			else -> "Sconosciuta"
		}
	}

	private fun mapPluggedToString(plugged: Int): String {
		return when (plugged) {
			BatteryManager.BATTERY_PLUGGED_AC -> "Corrente (AC)"
			BatteryManager.BATTERY_PLUGGED_USB -> "USB"
			BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
			else -> "Non in carica"
		}
	}

	@SuppressLint("DefaultLocale")
    private fun formatChargeTime(millis: Long): String {
		if (millis <= 0) return "Calcolo..."
		val hours = TimeUnit.MILLISECONDS.toHours(millis)
		val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
		return when {
			hours > 0 -> String.format("%d h %d min", hours, minutes)
			minutes > 0 -> String.format("%d min", minutes)
			else -> "Quasi completa"
		}
	}

	override fun onClick() {}
	override fun onLeftSwipe() {}
	override fun onRightSwipe() {}
	@Composable
	override fun PermissionsRequired() {}
}