package com.anto426.dynamicisland.plugins

import android.util.Log
import com.anto426.dynamicisland.model.service.IslandOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PluginActivity(
    val plugin: BasePlugin,
    var priority: PluginPriority,
    var lastUpdatedAt: Long = System.currentTimeMillis(),
    var timeoutMs: Long = 0L
)

object PluginManager {
    private const val TAG = "PluginManager"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var service: IslandOverlayService? = null
    private var lastTopId: String? = null

    private val activities = LinkedHashMap<String, PluginActivity>()
    private val timers = HashMap<String, kotlinx.coroutines.Job>()

    fun attach(service: IslandOverlayService) {
        this.service = service
        Log.d(TAG, "Attached to overlay service")
    }

    fun detach() {
        scope.cancel()
        activities.clear()
        timers.values.forEach { it.cancel() }
        timers.clear()
        service = null
    }

    fun submit(plugin: BasePlugin, priority: PluginPriority, timeoutMs: Long = 0L) {
        val now = System.currentTimeMillis()
        plugin.priority.value = priority
        plugin.lastUpdatedAt.value = now
        val act = activities[plugin.id]
        if (act == null) {
            activities[plugin.id] = PluginActivity(plugin, priority, now, timeoutMs)
        } else {
            act.priority = priority
            act.lastUpdatedAt = now
            act.timeoutMs = timeoutMs
        }
        startTimerIfNeeded(plugin.id, timeoutMs)
        recomputeTop()
    }

    fun refresh(plugin: BasePlugin) {
        activities[plugin.id]?.let {
            it.lastUpdatedAt = System.currentTimeMillis()
            recomputeTop()
        }
    }

    fun end(plugin: BasePlugin) {
        activities.remove(plugin.id)
        timers.remove(plugin.id)?.cancel()
        recomputeTop()
    }

    private fun startTimerIfNeeded(pluginId: String, timeoutMs: Long) {
        timers.remove(pluginId)?.cancel()
        if (timeoutMs <= 0L) return
        timers[pluginId] = scope.launch {
            delay(timeoutMs)
            activities[pluginId]?.plugin?.let { end(it) }
        }
    }

    private fun recomputeTop() {
        val top = activities.values
            .sortedWith(compareByDescending<PluginActivity> { it.priority.weight }
                .thenByDescending { it.lastUpdatedAt })
            .firstOrNull()?.plugin
        val newTopId = top?.id
        if (newTopId != lastTopId) {
            lastTopId = newTopId
            service?.setTopFromManager(top)
            Log.d(TAG, "Top: ${top?.id} | all: ${activities.values.map { it.plugin.id to it.priority }}")
        }
    }
}
