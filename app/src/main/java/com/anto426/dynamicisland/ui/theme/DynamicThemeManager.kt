package com.anto426.dynamicisland.ui.theme

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.anto426.dynamicisland.island.IslandSettings

object DynamicThemeManager {

    private var batteryLevel by mutableIntStateOf(100)
    private var isCharging by mutableStateOf(false)

    fun init(context: Context) {
        updateBatteryInfo(context)

        // Registra un receiver per gli aggiornamenti della batteria
        val batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryInfo(context)
            }
        }

        context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    private fun updateBatteryInfo(context: Context) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            batteryLevel = (level * 100) / scale
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    @Composable
    fun getDynamicColors(): DynamicColors {
        if (!IslandSettings.instance.dynamicThemeEnabled) {
            return DynamicColors.Default
        }

        return when {
            isCharging -> DynamicColors.Charging
            batteryLevel <= 15 -> DynamicColors.Critical
            batteryLevel <= 30 -> DynamicColors.Low
            else -> DynamicColors.Normal
        }
    }
}

sealed class DynamicColors {
    abstract val primary: Color
    abstract val secondary: Color
    abstract val surface: Color

    object Default : DynamicColors() {
        override val primary = Color(0xFF6750A4)
        override val secondary = Color(0xFF625B71)
        override val surface = Color(0xFFFEF7FF)
    }

    object Normal : DynamicColors() {
        override val primary = Color(0xFF4CAF50)  // Verde
        override val secondary = Color(0xFF388E3C)
        override val surface = Color(0xFFE8F5E8)
    }

    object Low : DynamicColors() {
        override val primary = Color(0xFFFF9800)  // Arancione
        override val secondary = Color(0xFFF57C00)
        override val surface = Color(0xFFFFF3E0)
    }

    object Critical : DynamicColors() {
        override val primary = Color(0xFFF44336)  // Rosso
        override val secondary = Color(0xFFD32F2F)
        override val surface = Color(0xFFFFEBEE)
    }

    object Charging : DynamicColors() {
        override val primary = Color(0xFF2196F3)  // Blu
        override val secondary = Color(0xFF1976D2)
        override val surface = Color(0xFFE3F2FD)
    }
}
