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
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anto426.dynamicisland.model.BATTERY_SHOW_PERCENTAGE
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginPriority
import com.anto426.dynamicisland.plugins.PluginSettingsItem
import com.anto426.dynamicisland.ui.theme.BatteryEmpty
import com.anto426.dynamicisland.ui.theme.BatteryFull
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.ui.island.PluginDefaults
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import com.anto426.dynamicisland.ui.island.SectionCard
import kotlin.math.PI
import kotlin.math.sin

private enum class DisplayMode {
    CHARGING, LOW_BATTERY, POWER_SAVER
}

val PowerSaverYellow = Color(0xFFFBC02D)

class BatteryShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cornerRadius = CornerRadius(size.width * 0.1f, size.width * 0.1f)
        val terminalHeight = size.height * 0.05f
        val terminalWidth = size.width * 0.4f
        val bodyHeight = size.height - terminalHeight

        path.addRoundRect(
            roundRect = RoundRect(
                rect = Rect(
                    offset = Offset(0f, terminalHeight),
                    size = Size(size.width, bodyHeight)
                ), cornerRadius = cornerRadius
            )
        )
        path.addRoundRect(
            roundRect = RoundRect(
                rect = Rect(
                    offset = Offset((size.width - terminalWidth) / 2, 0f),
                    size = Size(terminalWidth, terminalHeight)
                ), cornerRadius = CornerRadius(cornerRadius.x / 2, cornerRadius.y / 2)
            )
        )
        return Outline.Generic(path)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
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
    override var pluginSettings: SnapshotStateMap<String, PluginSettingsItem> = mutableStateMapOf(),
    override val sourceCodeUrl: String = "https://github.com/Anto426/Dynamic-Island/blob/main/app/src/main/java/com/anto426/dynamicisland/plugins/battery/BatteryPlugin.kt"

) : BasePlugin() {
    override val nameRes: Int? get() = R.string.plugin_battery_name
    override val descriptionRes: Int? get() = R.string.plugin_battery_description

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
            when (newDisplayMode) {
                DisplayMode.LOW_BATTERY -> {
                    // Cap battery below notifications
                    priority.value = PluginPriority.HIGH
                    show(context)
                }

                DisplayMode.CHARGING -> {
                    priority.value = PluginPriority.CRITICAL
                    show(context)
                }

                DisplayMode.POWER_SAVER -> {
                    priority.value = PluginPriority.MEDIUM
                    show(context)
                }

                null -> hide(context)
            }
        }
    }

    private val batteryBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            batteryPercent = (level * 100 / scale)
            isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging) {
                chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
                batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                batteryHealth =
                    mapHealthToString(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0))
                chargingSource =
                    mapPluggedToString(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
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

        // Popola (idempotente) le impostazioni in modo reattivo
        initSettings(context)
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
            animationSpec = tween(1200, easing = EaseOutCubic),
            label = "BatteryProgress"
        ).value
    // Percentage will be shown below the battery, so on-battery contrast color is no longer needed here.

        // Phases for motion effects
        val shimmerPhase by rememberInfiniteTransition(label = "BatteryShimmer")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ShimmerPhase"
            )
        val wavePhase by rememberInfiniteTransition(label = "BatteryWave")
            .animateFloat(
                initialValue = 0f,
                targetValue = (2f * PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = if (isCharging) 1800 else 2600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "WavePhase"
            )

        Column(
            modifier = Modifier
				.fillMaxSize()
                .padding(PluginDefaults.ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(0.55f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
    					.fillMaxSize()
    					.clip(BatteryShape())
                    .background(progressColor.copy(alpha = 0.18f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        BatteryShape()
                    )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val levelTop = size.height * (1f - animatedProgress)
                        val fillHeight = size.height - levelTop
                        if (fillHeight <= 0f) return@Canvas

                        // Liquid wave fill path
                        val waveAmplitude = size.height * if (isCharging) 0.03f else 0.02f
                        val wavelength = size.width * 0.9f
                        val path = Path().apply {
                            moveTo(0f, size.height)
                            lineTo(0f, levelTop)
                            var x = 0f
                            while (x <= size.width) {
                                val y: Double = levelTop + waveAmplitude * sin((2f * PI * (x / wavelength)) + wavePhase)
                                lineTo(x, y.toFloat())
                                x += size.width / 60f
                            }
                            lineTo(size.width, size.height)
                            close()
                        }

                        // Base gradient for the liquid
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    progressColor.copy(alpha = 0.9f),
                                    progressColor
                                ),
                                startY = levelTop,
                                endY = size.height
                            )
                        )

                        // Subtle inner edge highlight
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            style = Stroke(width = 2f)
                        )

                        // Moving shimmer band when charging
                        if (isCharging) {
                            val bandWidth = size.minDimension * 0.35f
                            val offsetX = (shimmerPhase * (size.width + bandWidth)) - bandWidth / 2f
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0f)
                                    ),
                                    start = Offset(offsetX - bandWidth, levelTop),
                                    end = Offset(offsetX + bandWidth, levelTop + bandWidth)
                                ),
                                topLeft = Offset(0f, levelTop),
                                size = Size(size.width, fillHeight)
                            )
                        }

                        // Glass overlay
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                                startY = size.height * 0.15f,
                                endY = size.height * 0.45f
                            ),
                            size = size
                        )
                    }

                    // Center capsule with optional state icon only
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        overlayIcon?.let {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                tonalElevation = 2.dp,
                                border = BorderStroke(1.dp, progressColor.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        tint = progressColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // Overlay moved into the battery shell
            }
            // Percentage shown below the battery graphic
            Text(
                text = "$batteryPercent%",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = progressColor
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            actions()
        }
    }

    @Composable
    private fun ChargingView() {
        val progressColor = lerp(BatteryEmpty, BatteryFull, batteryPercent / 100f)
    BatteryStatusView(
            progressColor = progressColor,
            title = context.getString(R.string.battery_charging_title),
            subtitle = formatChargeTime(chargeTimeRemaining),
            overlayIcon = Icons.Rounded.Bolt,
            actions = {
        SectionCard(
            title = context.getString(R.string.go_to_details),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(
                            Icons.Rounded.DeviceThermostat,
                            context.getString(R.string.temperature),
                            "${"%.1f".format(batteryTemperature)}°C"
                        )
                        InfoRow(
                            Icons.Rounded.Power,
                            context.getString(R.string.power_source),
                            chargingSource
                        )
                        InfoRow(
                            Icons.Rounded.HealthAndSafety,
                            context.getString(R.string.health),
                            batteryHealth
                        )
                    }
                }
            }
        )
    }

    @SuppressLint("StringFormatMatches")
    @Composable
    private fun LowBatteryView() {
    BatteryStatusView(
            progressColor = BatteryEmpty,
            title = context.getString(R.string.battery_low_title),
            subtitle = context.getString(R.string.battery_remaining_subtitle, batteryPercent),
            actions = {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { context.removePlugin(this@BatteryPlugin) }) {
                            Text(stringResource(id = R.string.ignore))
                        }
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            context.shrink()
                        }) {
                            Text(stringResource(id = R.string.battery_saver))
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun PowerSaverView() {
        BatteryStatusView(
            progressColor = PowerSaverYellow,
            title = context.getString(R.string.battery_saver),
            subtitle = context.getString(R.string.power_saver_active_desc),
            overlayIcon = Icons.Rounded.EnergySavingsLeaf,
            actions = {}
        )
    }

    @Composable
    override fun LeftOpenedComposable() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val (bg, fg, icon) = when (displayMode) {
                DisplayMode.CHARGING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Rounded.Bolt)
                DisplayMode.LOW_BATTERY -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Rounded.BatteryAlert)
                DisplayMode.POWER_SAVER -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Rounded.EnergySavingsLeaf)
                else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.BatteryStd)
            }
            Surface(
                shape = CircleShape,
                color = bg,
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    @Composable
    override fun RightOpenedComposable() {
        val showPercentage by remember {
            derivedStateOf {
                (pluginSettings[BATTERY_SHOW_PERCENTAGE] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value
                    ?: true
            }
        }
        if (showPercentage) {
            val color = when (displayMode) {
                DisplayMode.CHARGING -> lerp(BatteryEmpty, BatteryFull, batteryPercent / 100f)
                DisplayMode.LOW_BATTERY -> BatteryEmpty
                DisplayMode.POWER_SAVER -> PowerSaverYellow
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$batteryPercent%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }

    // React to setting changes coming from the Settings UI
    override fun onSettingsChanged(context: Context, key: String, value: Any?) {
        when (key) {
            "battery_show_temperature" -> (pluginSettings[key] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value =
                (value as? Boolean) ?: false

            "battery_low_notification" -> (pluginSettings[key] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value =
                (value as? Boolean) ?: false

            BATTERY_SHOW_PERCENTAGE -> (pluginSettings[key] as? PluginSettingsItem.SwitchSettingsItem)?.value?.value =
                (value as? Boolean) ?: true
        }
    }

    override fun onDestroy() {
        if (!::context.isInitialized) return
        try {
            context.unregisterReceiver(batteryBroadcastReceiver)
            context.unregisterReceiver(powerSaveModeReceiver)
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun mapHealthToString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.battery_health_failure)
            else -> context.getString(R.string.battery_health_unknown)
        }
    }

    private fun mapPluggedToString(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.battery_source_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.battery_source_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.battery_source_wireless)
            else -> context.getString(R.string.battery_source_not_charging)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatChargeTime(millis: Long): String {
        if (millis <= 0) return context.getString(R.string.battery_calculating)
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> context.getString(R.string.charge_time_h_m, hours, minutes)
            minutes > 0 -> context.getString(R.string.charge_time_m, minutes)
            else -> context.getString(R.string.battery_almost_full)
        }
    }

    override fun initSettings(context: Context) {
        // Evita di ricreare se già presenti
        if (pluginSettings.isNotEmpty()) {
            // Aggiorna i valori correnti da preferenze
            pluginSettings.values.forEach { item ->
                if (item is PluginSettingsItem.SwitchSettingsItem) {
                    item.value.value = item.isSettingEnabled(context, item.id)
                }
            }
            return
        }
        // Impostazione per mostrare la temperatura
        pluginSettings["battery_show_temperature"] = PluginSettingsItem.SwitchSettingsItem(
            id = "battery_show_temperature",
            title = context.getString(R.string.battery_setting_show_temperature_title),
            description = context.getString(R.string.battery_setting_show_temperature_desc),
            value = mutableStateOf(true)
        )

        // Impostazione per mostrare la percentuale nella vista compatta
        pluginSettings[BATTERY_SHOW_PERCENTAGE] = PluginSettingsItem.SwitchSettingsItem(
            id = BATTERY_SHOW_PERCENTAGE,
            title = context.getString(R.string.battery_setting_show_percentage_title),
            description = context.getString(R.string.battery_setting_show_percentage_desc),
            value = mutableStateOf(true)
        )

        // Impostazione per notifiche batteria bassa
        pluginSettings["battery_low_notification"] = PluginSettingsItem.SwitchSettingsItem(
            id = "battery_low_notification",
            title = context.getString(R.string.battery_setting_low_notification_title),
            description = context.getString(R.string.battery_setting_low_notification_desc),
            value = mutableStateOf(true)
        )

        // Impostazione per soglia batteria bassa personalizzata - COMMENTATA per ora
        /*
        pluginSettings["battery_custom_threshold"] = PluginSettingsItem.SliderSettingsItem(
            id = "battery_custom_threshold",
            title = "Soglia Batteria Bassa",
            description = "Imposta la percentuale per le notifiche di batteria bassa",
            value = mutableStateOf(20f),
            range = 10f..30f,
            steps = 20,
            isSettingEnabled = { context, id -> getFloatSetting(context, id, 20f) },
            onValueChange = { context, value ->
                saveFloatSetting(context, id, value)
                LOW_BATTERY_THRESHOLD = value.toInt()
            }
        )
        */

        // Sincronizza i valori iniziali con le preferenze
        pluginSettings.values.forEach { item ->
            if (item is PluginSettingsItem.SwitchSettingsItem) {
                item.value.value = item.isSettingEnabled(context, item.id)
            }
        }
    }

    private fun getBooleanSetting(context: Context, key: String, default: Boolean): Boolean {
        val prefs = context.getSharedPreferences("battery_plugin_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(key, default)
    }

    private fun saveBooleanSetting(context: Context, key: String, value: Boolean) {
        val prefs = context.getSharedPreferences("battery_plugin_prefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean(key, value) }
    }

    private fun getFloatSetting(context: Context, key: String, default: Float): Float {
        val prefs = context.getSharedPreferences("battery_plugin_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat(key, default)
    }

    private fun saveFloatSetting(context: Context, key: String, value: Float) {
        val prefs = context.getSharedPreferences("battery_plugin_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat(key, value).apply()
    }

    // Implementazione metodi astratti mancanti
    override fun onClick() {
        // Espandi l'isola quando si clicca sulla batteria
        context.expand()
    }

    @Composable
    override fun PermissionsRequired() {
        // La batteria non richiede permessi speciali
    }

    override fun onRightSwipe() {
        // Swipe destra - potrebbe aprire impostazioni batteria
        context.expand()
    }

    override fun onLeftSwipe() {
        // Swipe sinistra - potrebbe chiudere l'isola
        context.shrink()
    }
}