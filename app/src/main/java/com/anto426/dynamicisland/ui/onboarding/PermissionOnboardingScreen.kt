package com.anto426.dynamicisland.ui.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.model.service.IslandOverlayService
import com.anto426.dynamicisland.model.service.NotificationService
import kotlinx.coroutines.delay
import androidx.core.net.toUri

data class PermissionItemData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = false,
    val category: String = "",
    val settingsAction: String? = null,
    val canRequestDirectly: Boolean = false,
    val permissionName: String? = null,
    val checkFunction: (Context) -> Boolean
)

private sealed class OnboardingStep {
    object Introduction : OnboardingStep()
    object Permissions : OnboardingStep()
}

@Composable
fun OnboardingFlowScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf<OnboardingStep>(OnboardingStep.Introduction) }
    var showCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(2000)
            onComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            }, label = "OnboardingFlow"
        ) { step ->
            when (step) {
                is OnboardingStep.Introduction -> IntroScreen { currentStep = OnboardingStep.Permissions }
                is OnboardingStep.Permissions -> PermissionOnboardingScreen(
                    onPermissionsComplete = { showCelebration = true }
                )
            }
        }

        if (showCelebration) {
            CelebrationScreen()
        }
    }
}

@Composable
private fun IntroScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = stringResource(id = R.string.about_app_icon_description),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = stringResource(id = R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.onboarding_welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(id = R.string.get_started), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun PermissionOnboardingScreen(
    onPermissionsComplete: () -> Unit
) {
    val appContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionCategories = remember {
        listOf(
            listOf(
                PermissionItemData(title = appContext.getString(R.string.permission_accessibility_title), description = appContext.getString(R.string.permission_accessibility_desc), icon = Icons.Rounded.Accessibility, isRequired = true, category = appContext.getString(R.string.permission_category_system), settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS, checkFunction = { ctx -> try { val enabledServices = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES); val component = ComponentName(ctx, IslandOverlayService::class.java).flattenToString(); enabledServices?.split(":")?.any { it.equals(component, ignoreCase = true) } == true } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_overlay_title), description = appContext.getString(R.string.permission_overlay_desc), icon = Icons.Rounded.Layers, isRequired = true, category = appContext.getString(R.string.permission_category_system), settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION, checkFunction = { ctx -> try { Settings.canDrawOverlays(ctx) } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_battery_title), description = appContext.getString(R.string.permission_battery_desc), icon = Icons.Rounded.BatteryStd, isRequired = true, category = appContext.getString(R.string.permission_category_system), settingsAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, checkFunction = { ctx -> try { val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager; powerManager?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false } catch (e: Exception) { false } })
            ),
            listOf(
                PermissionItemData(title = appContext.getString(R.string.permission_notification_listener_title), description = appContext.getString(R.string.permission_notification_listener_desc), icon = Icons.Rounded.Notifications, isRequired = true, category = appContext.getString(R.string.permission_category_notifications), settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, checkFunction = { ctx -> try { val enabledListeners = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners"); val component = ComponentName(ctx, NotificationService::class.java).flattenToString(); enabledListeners?.split(":")?.any { it.equals(component, ignoreCase = true) } == true } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_post_notifications_title), description = appContext.getString(R.string.permission_post_notifications_desc), icon = Icons.Rounded.Notifications, isRequired = false, category = appContext.getString(R.string.permission_category_notifications), canRequestDirectly = true, permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@PermissionItemData true; ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_vibrate_title), description = appContext.getString(R.string.permission_vibrate_desc), icon = Icons.Rounded.Vibration, isRequired = false, category = appContext.getString(R.string.permission_category_notifications), permissionName = Manifest.permission.VIBRATE, canRequestDirectly = true, checkFunction = { ctx -> try { ContextCompat.checkSelfPermission(ctx, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } })
            ),
            listOf(
                PermissionItemData(title = appContext.getString(R.string.permission_storage_legacy_title), description = appContext.getString(R.string.permission_storage_legacy_desc), icon = Icons.Rounded.Storage, isRequired = false, category = appContext.getString(R.string.permission_category_storage), permissionName = Manifest.permission.READ_EXTERNAL_STORAGE, canRequestDirectly = true, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return@PermissionItemData true; ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_media_images_title), description = appContext.getString(R.string.permission_media_images_desc), icon = Icons.Rounded.Image, isRequired = false, category = appContext.getString(R.string.permission_category_storage), permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else null, canRequestDirectly = true, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@PermissionItemData true; ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_media_video_title), description = appContext.getString(R.string.permission_media_video_desc), icon = Icons.Rounded.Image, isRequired = false, category = appContext.getString(R.string.permission_category_storage), permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else null, canRequestDirectly = true, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@PermissionItemData true; ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } }),
                PermissionItemData(title = appContext.getString(R.string.permission_media_audio_title), description = appContext.getString(R.string.permission_media_audio_desc), icon = Icons.Rounded.MusicNote, isRequired = false, category = appContext.getString(R.string.permission_category_storage), permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else null, canRequestDirectly = true, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@PermissionItemData true; ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false } })
            ),
            listOf(
                PermissionItemData(title = appContext.getString(R.string.permission_unknown_sources_title), description = appContext.getString(R.string.permission_unknown_sources_desc), icon = Icons.Rounded.GetApp, isRequired = false, category = appContext.getString(R.string.permission_category_updates), permissionName = null, canRequestDirectly = false, settingsAction = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, checkFunction = { ctx -> try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { ctx.packageManager.canRequestPackageInstalls() } else { true } } catch (e: Exception) { false } })
            )
        )
    }

    var pendingPermissions by remember { mutableStateOf(permissionCategories.flatten().filter { !it.checkFunction(appContext) }) }
    var currentStep by remember { mutableStateOf(0) }
    val grantedStatusMap = remember { mutableStateMapOf<String, Boolean>() }

    val refreshPermissionsStatus = {
        val newPending = mutableListOf<PermissionItemData>()
        val allPermissions = permissionCategories.flatten()
        allPermissions.forEach { permission ->
            val isGranted = permission.checkFunction(appContext)
            grantedStatusMap[permission.title] = isGranted
            if (!isGranted) newPending.add(permission)
        }
        pendingPermissions = newPending
        if (currentStep >= newPending.size) {
            currentStep = (newPending.size - 1).coerceAtLeast(0)
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissionsStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionsStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (pendingPermissions.isEmpty()) {
        LaunchedEffect(Unit) {
            onPermissionsComplete()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
            }, label = "PermissionStepAnimation"
        ) { step ->
            val permission = pendingPermissions.getOrNull(step)
            if (permission != null) {
                PermissionStepContent(
                    permission = permission,
                    step = permissionCategories.flatten().indexOf(permission) + 1,
                    totalSteps = permissionCategories.flatten().size,
                    isGranted = grantedStatusMap[permission.title] ?: false
                )
            }
        }

    }
}

@Composable
private fun PermissionStepContent(
    permission: PermissionItemData,
    step: Int,
    totalSteps: Int,
    isGranted: Boolean
) {
    val appContext = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Lo stato verrà aggiornato dal LifecycleObserver del genitore */ }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_step_counter, step, totalSteps),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionCard(
            permission = permission,
            isGranted = isGranted,
            onGrantClick = {
                val permissionName = permission.permissionName
                if (permission.canRequestDirectly && permissionName != null) {
                    permissionLauncher.launch(permissionName)
                } else if (permission.settingsAction != null) {
                    try {
                        val intent = Intent(permission.settingsAction).apply {
                            if (permission.settingsAction in listOf(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)) {
                                data = "package:${appContext.packageName}".toUri()
                            }
                        }
                        appContext.startActivity(intent)
                    } catch (e: Exception) { /* Gestisci errore */ }
                }
            }
        )
    }
}

@Composable
private fun PermissionCard(permission: PermissionItemData, isGranted: Boolean, onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = permission.icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(text = permission.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text = permission.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isGranted,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                if (isGranted) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(id = R.string.onboarding_permission_granted))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = if (isGranted) stringResource(id = R.string.permission_granted_label) else stringResource(id = R.string.enable_in_settings), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}



@Composable
private fun CelebrationScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
            Text(stringResource(id = R.string.onboarding_setup_complete_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(id = R.string.onboarding_setup_complete_desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}