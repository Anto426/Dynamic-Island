package com.anto426.dynamicisland.plugins.battery

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler // NUOVO: Import necessario
import android.os.Looper // NUOVO: Import necessario
import android.os.PowerManager
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
import java.util.concurrent.TimeUnit

private enum class DisplayMode {
	CHARGING, LOW_BATTERY, POWER_SAVER
}

val PowerSaverYellow = Color(0xFFFBC02D)

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
	override val id: String = "battery",
	override val name: String = "Batteria",
	override val description: String = "Mostra lo stato della batteria",
	override val author: String = "Anto426",
	override val version: String = "1.0.0",
	override val permissions: ArrayList<String> = arrayListOf(),
	override var enabled: MutableState<Boolean> = mutableStateOf(false),
	override var pluginSettings: MutableMap<String, PluginSettingsItem> = mutableMapOf(),
	override val sourceCodeUrl:String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/battery/BatteryPlugin.kt"

) : BasePlugin() {

	private lateinit var context: IslandOverlayService
	private lateinit var batteryManager: BatteryManager
	private lateinit var powerManager: PowerManager

	private var batteryPercent by mutableStateOf(0)
	private var isCharging by mutableStateOf(false)
	private var isPowerSaveModeOn by mutableStateOf(false)
	private var chargeTimeRemaining by mutableStateOf(-1L)
	private var batteryTemperature by mutableStateOf(0f)
	private var batteryHealth by mutableStateOf("")
	private var chargingSource by mutableStateOf("")

	private var displayMode by mutableStateOf<DisplayMode?>(null)
	private val LOW_BATTERY_THRESHOLD = 20
	private var lowBatteryNotified by mutableStateOf(false)

	private fun updateDisplayState() {
		val isLow = batteryPercent <= LOW_BATTERY_THRESHOLD

		if (isCharging || !isLow) {
			lowBatteryNotified = false
		}

		val newDisplayMode = when {
			isCharging -> DisplayMode.CHARGING
			isPowerSaveModeOn -> DisplayMode.POWER_SAVER
			isLow && !lowBatteryNotified -> {
				lowBatteryNotified = true
				DisplayMode.LOW_BATTERY
			}
			else -> null
		}

		if (newDisplayMode != displayMode) {
			displayMode = newDisplayMode
			if (newDisplayMode != null) {
				context.addPlugin(this@BatteryPlugin)
			} else {
				context.removePlugin(this@BatteryPlugin)
			}
		}
	}

	private val batteryBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
			val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
			val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

			batteryPercent = (level * 100 / scale)
			isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

			if (isCharging) {
				chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
				batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
				batteryHealth = mapHealthToString(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0))
				chargingSource = mapPluggedToString(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
			}

			updateDisplayState()
		}
	}

	private val powerSaveModeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(c: Context, i: Intent) {
			isPowerSaveModeOn = powerManager.isPowerSaveMode
			updateDisplayState()
		}
	}

	override fun canExpand(): Boolean = true

	override fun onCreate(context: IslandOverlayService?) {
		this.context = context ?: return
		this.batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
		this.powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

		isPowerSaveModeOn = powerManager.isPowerSaveMode

		val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
		context.registerReceiver(batteryBroadcastReceiver, batteryFilter)

		val powerSaverFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
		context.registerReceiver(powerSaveModeReceiver, powerSaverFilter)

		// ====================================================================================
		// NUOVA MODIFICA: Aggiungiamo un piccolo ritardo al controllo iniziale
		// per dare tempo alla UI di prepararsi completamente.
		// ====================================================================================
		Handler(Looper.getMainLooper()).postDelayed({
			// Questo codice verrà eseguito dopo 500ms
			context.registerReceiver(null, batteryFilter)?.let { initialIntent ->
				batteryBroadcastReceiver.onReceive(context, initialIntent)
			}
		}, 500) // Ritardo di 500 millisecondi

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
			DisplayMode.POWER_SAVER -> PowerSaverView()
			null -> {}
		}
	}

	@Composable
	private fun BatteryStatusView(
		progressColor: Color,
		title: String,
		subtitle: String,
		overlayIcon: ImageVector? = null,
		actions: @Composable () -> Unit = {}
	) {
		val animatedProgress = animateFloatAsState(
			targetValue = batteryPercent / 100f,
			animationSpec = tween(1000),
			label = "BatteryProgress"
		).value
		val textColorOnBattery = if (progressColor.luminance() > 0.5)
			MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
		else
			Color.White

		Column(
			modifier = Modifier.fillMaxSize().padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.SpaceBetween
		) {
			Box(
				modifier = Modifier.weight(1f, fill = false).aspectRatio(0.55f),
				contentAlignment = Alignment.Center
			) {
				Box(
					modifier = Modifier.fillMaxSize().clip(BatteryShape())
						.background(progressColor.copy(alpha = 0.3f)),
					contentAlignment = Alignment.BottomCenter
				) {
					Box(
						modifier = Modifier.fillMaxWidth().fillMaxHeight(animatedProgress)
							.background(progressColor)
					)
				}
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					overlayIcon?.let {
						Icon(
							imageVector = it,
							contentDescription = title,
							tint = textColorOnBattery,
							modifier = Modifier.size(40.dp)
						)
						Spacer(modifier = Modifier.height(8.dp))
					}
					Text(
						text = "$batteryPercent%",
						style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
						color = textColorOnBattery
					)
				}
			}

			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier.padding(vertical = 24.dp)
			) {
				Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(4.dp))
				Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
			}

			Box(modifier = Modifier.padding(bottom = 8.dp)) {
				actions()
			}
		}
	}

	@Composable
	private fun ChargingView() {
		val progressColor = lerp(BatteryEmpty, BatteryFull, batteryPercent / 100f)
		BatteryStatusView(
			progressColor = progressColor,
			title = "In Carica",
			subtitle = formatChargeTime(chargeTimeRemaining),
			overlayIcon = Icons.Rounded.Bolt,
			actions = {
				Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
					InfoRow(Icons.Rounded.DeviceThermostat, "Temperatura", "${"%.1f".format(batteryTemperature)}°C")
					InfoRow(Icons.Rounded.Power, "Fonte", chargingSource)
					InfoRow(Icons.Rounded.HealthAndSafety, "Salute", batteryHealth)
				}
			}
		)
	}

	@Composable
	private fun LowBatteryView() {
		BatteryStatusView(
			progressColor = BatteryEmpty,
			title = "Batteria Scarica",
			subtitle = "$batteryPercent% rimanente",
			actions = {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
					verticalAlignment = Alignment.CenterVertically
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
		)
	}

	@Composable
	private fun PowerSaverView() {
		BatteryStatusView(
			progressColor = PowerSaverYellow,
			title = "Risparmio Energetico",
			subtitle = "Modalità attiva per estendere la durata",
			overlayIcon = Icons.Rounded.EnergySavingsLeaf,
			actions = {}
		)
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
					Icon(imageVector = Icons.Rounded.Bolt, "Charging", tint = progressColor, modifier = Modifier.fillMaxSize())
				}
				DisplayMode.LOW_BATTERY -> {
					Icon(imageVector = Icons.Rounded.BatteryAlert, "Low Battery", tint = BatteryEmpty, modifier = Modifier.fillMaxSize())
				}
				DisplayMode.POWER_SAVER -> {
					Icon(imageVector = Icons.Rounded.EnergySavingsLeaf, "Power Saver", tint = PowerSaverYellow, modifier = Modifier.fillMaxSize())
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
				DisplayMode.POWER_SAVER -> PowerSaverYellow
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
			context.unregisterReceiver(powerSaveModeReceiver)
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