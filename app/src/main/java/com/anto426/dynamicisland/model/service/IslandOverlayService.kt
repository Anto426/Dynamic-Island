package com.anto426.dynamicisland.model.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.*
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.Intent.ACTION_USER_PRESENT
import android.content.Intent.ACTION_USER_UNLOCKED
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.view.accessibility.AccessibilityEvent
import android.app.KeyguardManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.anto426.dynamicisland.island.Island
import com.anto426.dynamicisland.island.IslandState
import com.anto426.dynamicisland.island.IslandStates
import com.anto426.dynamicisland.island.IslandSettings
import com.anto426.dynamicisland.island.IslandViewState
import com.anto426.dynamicisland.model.*
import com.anto426.dynamicisland.plugins.BasePlugin
import com.anto426.dynamicisland.plugins.PluginPriority
import com.anto426.dynamicisland.plugins.PluginManager
import com.anto426.dynamicisland.plugins.PluginEngine
import com.anto426.dynamicisland.plugins.ExportedPlugins
import com.anto426.dynamicisland.ui.island.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.anto426.dynamicisland.R


class IslandOverlayService : AccessibilityService() {

	private val params = WindowManager.LayoutParams(
		WRAP_CONTENT,
		WRAP_CONTENT,
		TYPE_ACCESSIBILITY_OVERLAY,
		FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS or FLAG_NOT_TOUCH_MODAL or FLAG_NOT_FOCUSABLE,
		PixelFormat.TRANSLUCENT
	).apply {
		gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
	}

	private lateinit var settingsPreferences: SharedPreferences

	// State of the overlay
	var islandState : IslandState by mutableStateOf(IslandViewState.Closed)
		private set

	// Plugins
	private val plugins: ArrayList<BasePlugin> = ExportedPlugins.plugins
	val bindedPlugins = mutableStateListOf<BasePlugin>()
	private val initializedPlugins = mutableSetOf<String>()
	// Keep a stack to support fallback when a higher priority plugin hides
	private val activeStack = ArrayDeque<BasePlugin>()

	// Theme
	var invertedTheme by mutableStateOf(false)

	// Auto-hide removed
	private var autoHideJob: Job? = null // kept for compatibility, unused

	// Debounced sync for SETTINGS_CHANGED to avoid thrashing the engine
	private val serviceScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + SupervisorJob())
	private var syncJob: Job? = null
	private fun scheduleSync() {
		syncJob?.cancel()
		syncJob = serviceScope.launch {
			delay(150)
			PluginEngine.sync(this@IslandOverlayService)
		}
	}

	companion object {
		private var instance: IslandOverlayService? = null

		fun getInstance(): IslandOverlayService? {
			return instance
		}
	}

	private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			when (intent.action) {
				SETTINGS_CHANGED -> {
					// Debounce sync to reduce churn
					scheduleSync()
				}
				SETTINGS_THEME_INVERTED -> {
					val settingsPreferences = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)
					invertedTheme = settingsPreferences.getBoolean(THEME_INVERTED, false)
				}
				ACTION_SCREEN_ON -> {
					Island.isScreenOn = true
					val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
					Island.isLocked = km.isKeyguardLocked
					// Re-evaluate visibility/state on screen on
					reevaluateState()
				}
				ACTION_SCREEN_OFF -> {
					Island.isScreenOn = false
					Island.isLocked = true
				}
				ACTION_USER_PRESENT, ACTION_USER_UNLOCKED -> {
					// Device just unlocked; ensure we show again based on normal visibility rules
					Island.isLocked = false
					reevaluateState()
				}
			}
		}
	}

	override fun onServiceConnected() {
		super.onServiceConnected()
		setTheme(R.style.Theme_DynamicIsland)
		instance = this
		settingsPreferences = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)

		// Register broadcast receiver
		registerReceiver(
			mBroadcastReceiver,
			IntentFilter().apply {
				addAction(SETTINGS_CHANGED)
				addAction(SETTINGS_THEME_INVERTED)
				addAction(ACTION_SCREEN_ON)
				addAction(ACTION_SCREEN_OFF)
				addAction(ACTION_USER_PRESENT)
				addAction(ACTION_USER_UNLOCKED)
			},
			RECEIVER_EXPORTED
		)

		// Setup plugins lifecycle and manager
		ExportedPlugins.setupPlugins(context = this)
		PluginManager.attach(this)
		PluginEngine.start(this)

		// Setup initial composition
		init()

		val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
		showOverlay(windowManager, params)
	}

	fun init() {
		// First-time clean state
		initializedPlugins.clear()
		bindedPlugins.clear()
		islandState = IslandViewState.Closed
		// PluginEngine will start needed plugins
		PluginEngine.sync(this)

		// Setup inverted theme
		val settingsPreferences = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)
		invertedTheme = settingsPreferences.getBoolean(THEME_INVERTED, false)
	}

	private fun incrementalInit() {
		// Delegate lifecycle sync to PluginEngine
		PluginEngine.sync(this)
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun showOverlay(
		windowManager: WindowManager,
		params: WindowManager.LayoutParams
	) {
		val composeView = ComposeView(this)
		// Add effects when notification received, swiped, etc.
        ComposeView(this)

		composeView.setContent {
			// Listen for plugin changes
			LaunchedEffect(bindedPlugins.firstOrNull()) {
				islandState = if (bindedPlugins.firstOrNull() != null) {
					if (Island.isLocked && !IslandSettings.instance.showOnLockScreen) {
						IslandViewState.Closed
					} else IslandViewState.Opened
				} else {
					IslandViewState.Closed
				}
				Log.d("OverlayService", "Plugins changed: $bindedPlugins")
				// Autohide removed
			}

			IslandApp(
				islandOverlayService = this,
			)
		}

		// TODO: Find a way to detect when a click is performed outside of the overlay (to close it)
		/*composeView.setOnTouchListener { view: View?, event: MotionEvent ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				Log.d("OverlayService", "Touch event")
			}
			false
		}*/

		// Trick The ComposeView into thinking we are tracking lifecycle
		/*val viewModelStore = ViewModelStore()
		val lifecycleOwner = MyLifecycleOwner()
		lifecycleOwner.performRestore(null)
		lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
		ViewTreeLifecycleOwner.set(composeView, lifecycleOwner)
		ViewTreeViewModelStoreOwner.set(composeView) { viewModelStore }
		composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)*/

		val viewModelStore = ViewModelStore()
		val viewModelStoreOwner = object : ViewModelStoreOwner {
			override val viewModelStore: ViewModelStore
				get() = viewModelStore
		}

		val lifecycleOwner = MyLifecycleOwner()
		lifecycleOwner.performRestore(null)
		lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
		composeView.setViewTreeLifecycleOwner(lifecycleOwner)
		composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
		composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)

		// Make recomposition happen on the UI thread
		val coroutineContext = AndroidUiDispatcher.CurrentThread
		val runRecomposeScope = CoroutineScope(coroutineContext)
		val recomposer = Recomposer(coroutineContext)
		composeView.compositionContext = recomposer
		runRecomposeScope.launch {
			recomposer.runRecomposeAndApplyChanges()
		}

		// Add the view to the window
		windowManager.addView(composeView, params)
	}

	fun addPlugin(plugin: BasePlugin) {
	// Update recency
	plugin.lastUpdatedAt.value = System.currentTimeMillis()

	// Push or move to top in active stack
	activeStack.removeIf { it.id == plugin.id }
	activeStack.addFirst(plugin)

	// Refresh visible list from stack applying priority ordering
	refreshVisibleFromStack()
	}
	fun removePlugin(plugin: BasePlugin) {
		Log.d("OverlayService", "Plugin with id ${plugin.id} removed")
		// Remove from stack and visible list, then restore next appropriate
		activeStack.removeIf { it.id == plugin.id }
		bindedPlugins.removeIf { it.id == plugin.id }
		refreshVisibleFromStack()
	}

	private fun refreshVisibleFromStack() {
		// Select the top-most plugin by priority then recency from the stack
		val top = activeStack
			.distinctBy { it.id }
			.sortedWith(compareByDescending<BasePlugin> { it.priority.value.weight }
				.thenByDescending { it.lastUpdatedAt.value })
			.firstOrNull()

		bindedPlugins.clear()
		if (top != null) bindedPlugins.add(top)

		islandState = if (top != null) {
			if (Island.isLocked && !IslandSettings.instance.showOnLockScreen) {
				IslandViewState.Closed
			} else IslandViewState.Opened
		} else IslandViewState.Closed
	// Autohide removed

		Log.d("OverlayService", "Top plugin: ${top?.id} | stack: ${activeStack.map { it.id to it.priority.value }}")
	}

	fun expand() { islandState = IslandViewState.Expanded(configuration = resources.configuration) }
	fun shrink() { islandState = IslandViewState.Opened }

	// Metodi per gestire le nuove impostazioni avanzate
	fun performHapticFeedback() {
		if (IslandSettings.instance.hapticFeedback) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				val vibrator = getSystemService(Vibrator::class.java)
				vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
			}
		}
	}

	fun playSound() {
		if (IslandSettings.instance.soundEnabled && !IslandSettings.instance.silentMode) {
			// Implementa riproduzione suono qui
			// Per ora è un placeholder
		}
	}

	fun shouldAnimate(): Boolean {
		// Honor animations toggle; low power mode suppresses animations without changing the stored value
		return IslandSettings.instance.animationsEnabled && !IslandSettings.instance.lowPowerMode
	}

	// Autohide removed: no delay-based closing

	override fun onUnbind(intent: Intent?): Boolean {
		instance = null
		return super.onUnbind(intent)
	}

	override fun onDestroy() {
		super.onDestroy()
		PluginEngine.stop()
		PluginManager.detach()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		Island.isInLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
	}

	override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
	override fun onInterrupt() {}

		// Called by PluginManager to set the top plugin deterministically
		fun setTopFromManager(top: BasePlugin?) {
			bindedPlugins.clear()
			if (top != null) bindedPlugins.add(top)
			// Respect lock-screen visibility preference
			islandState = if (top != null) {
				if (Island.isLocked && !IslandSettings.instance.showOnLockScreen) {
					IslandViewState.Closed
				} else IslandViewState.Opened
			} else IslandViewState.Closed
			// Autohide removed
		}

	private fun reevaluateState() {
		// Recompute state after lock/unlock or screen events
		val hasPlugin = bindedPlugins.firstOrNull() != null
		islandState = if (hasPlugin) {
			if (Island.isLocked && !IslandSettings.instance.showOnLockScreen) {
				IslandViewState.Closed
			} else IslandViewState.Opened
		} else {
			IslandViewState.Closed
		}
	}
}