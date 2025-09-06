package com.anto426.dynamicisland.plugins

import android.content.Context
import android.util.Log
import com.anto426.dynamicisland.model.SETTINGS_CHANGED
import com.anto426.dynamicisland.model.SETTINGS_KEY
import com.anto426.dynamicisland.model.service.IslandOverlayService


object PluginEngine {
    private const val TAG = "PluginEngine"

    private var service: IslandOverlayService? = null

    fun start(service: IslandOverlayService) {
        this.service = service
        sync(service)
        Log.d(TAG, "Engine started")
    }

    fun stop() {
        if (service == null) return
        ExportedPlugins.plugins.forEach { plugin ->
            if (plugin.isStarted.value) {
                runCatching { plugin.onDestroy() }
                plugin.isStarted.value = false
                PluginManager.end(plugin)
            }
        }
        service = null
        Log.d(TAG, "Engine stopped")
    }

    fun sync(context: Context) {
        val srv = service ?: return
        ExportedPlugins.setupPlugins(context)
        ExportedPlugins.plugins.forEach { plugin ->
            val shouldRun = plugin.active
            Log.d(TAG, "Plugin ${plugin.id}: enabled=${plugin.enabled.value}, permissionsGranted=${plugin.allPermissionsGranted}, active=$shouldRun, started=${plugin.isStarted.value}")
            if (shouldRun && !(plugin.isStarted.value)) {
                runCatching { plugin.onCreate(srv) }
                    .onSuccess {
                        plugin.isStarted.value = true
                        Log.d(TAG, "Started ${plugin.id}")
                    }
                    .onFailure { e -> Log.e(TAG, "Failed to start ${plugin.id}", e) }
            } else if (!shouldRun && (plugin.isStarted.value)) {
                runCatching { plugin.onDestroy() }
                plugin.isStarted.value = false
                PluginManager.end(plugin)
                Log.d(TAG, "Stopped ${plugin.id}")
            }
        }
    }
}
