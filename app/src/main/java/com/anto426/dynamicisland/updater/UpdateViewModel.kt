package com.anto426.dynamicisland.updater

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anto426.dynamicisland.updater.DownloadManager.DownloadCallback
import com.anto426.dynamicisland.updater.DownloadManager.DownloadResult
import com.anto426.dynamicisland.updater.LocalUpdateManager.ReleaseChannel
import com.anto426.dynamicisland.updater.LocalUpdateManager.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private lateinit var localUpdateManager: LocalUpdateManager
    private lateinit var downloadManager: DownloadManager

    private object PrefKeys {
        const val FILE = "update_prefs"
        const val DOWNLOADED_APK_PATH = "downloaded_apk_path"
        const val DOWNLOADED_APK_VERSION = "downloaded_apk_version"
    }

    data class UiState(
        val currentVersion: String = "1.0.0",
        val selectedChannel: ReleaseChannel = ReleaseChannel.STABLE,
        val updateCheckState: UpdateCheckState = UpdateCheckState.Idle,
        val downloadState: DownloadState = DownloadState.Idle,
        val isAutoUpdateEnabled: Boolean = true,
        val isAutoDownloadEnabled: Boolean = false
    )

    sealed class UpdateCheckState {
        object Idle : UpdateCheckState()
        object Checking : UpdateCheckState()
        data class UpdateAvailable(val updateInfo: LocalUpdateManager.UpdateInfo) : UpdateCheckState()
        data class Error(val message: String) : UpdateCheckState()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    fun initialize(context: Context) {
        localUpdateManager = LocalUpdateManager(context)
        downloadManager = DownloadManager(context)

        // Carica impostazioni iniziali
        _uiState.value = _uiState.value.copy(
            selectedChannel = localUpdateManager.getSelectedChannel(),
            isAutoUpdateEnabled = getAutoUpdateEnabled(context),
            isAutoDownloadEnabled = getAutoDownloadEnabled(context)
        )

        // Carica lo stato dell'ultimo controllo aggiornamenti
        loadSavedUpdateState(context)

        // Ottiene versione attuale
        getCurrentVersion(context)
    }

    private fun loadSavedUpdateState(context: Context) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

        // Carica se c'è un aggiornamento disponibile salvato
        val hasUpdateAvailable = prefs.getBoolean("has_update_available", false)
        if (hasUpdateAvailable) {
            val updateVersion = prefs.getString("update_version", null)
            val updateUrl = prefs.getString("update_url", null)
            val updateSize = prefs.getLong("update_size", 0L)
            val updateChecksum = prefs.getString("update_checksum", null)
            val updateChangelog = prefs.getString("update_changelog", null)
            val updateChannel = prefs.getString("update_channel", "stable")
            val updateVersionCode = prefs.getInt("update_version_code", 1)
            val updateReleaseNotes = prefs.getString("update_release_notes", null)
            val updateReleaseDate = prefs.getString("update_release_date", "")
            val updateMinSupportedVersion = prefs.getString("update_min_supported_version", "1.0.0")
            val updateForceUpdate = prefs.getBoolean("update_force_update", false)

            if (updateVersion != null && updateUrl != null) {
                val updateInfo = LocalUpdateManager.UpdateInfo(
                    channel = updateChannel,
                    latestVersion = updateVersion,
                    versionCode = updateVersionCode,
                    downloadUrl = updateUrl,
                    releaseNotes = updateReleaseNotes,
                    releaseDate = updateReleaseDate,
                    minimumSupportedVersion = updateMinSupportedVersion,
                    forceUpdate = updateForceUpdate,
                    checksum = updateChecksum,
                    fileSize = updateSize,
                    changelog = if (updateChangelog != null) {
                        listOf(LocalUpdateManager.ChangelogEntry(
                            version = updateVersion,
                            date = "Salvato",
                            changes = listOf(updateChangelog)
                        ))
                    } else emptyList()
                )

                _uiState.value = _uiState.value.copy(
                    updateCheckState = UpdateCheckState.UpdateAvailable(updateInfo)
                )
            }
        }
    }

    private fun getCurrentVersion(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = packageInfo.versionName ?: "1.0.0"
            _uiState.value = _uiState.value.copy(currentVersion = version)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(currentVersion = "1.0.0")
        }
    }

    private fun saveUpdateState(context: Context, updateInfo: LocalUpdateManager.UpdateInfo?) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (updateInfo != null) {
                putBoolean("has_update_available", true)
                putString("update_version", updateInfo.latestVersion)
                putString("update_url", updateInfo.downloadUrl)
                putLong("update_size", updateInfo.fileSize ?: 0L)
                putString("update_checksum", updateInfo.checksum)
                putString("update_changelog", updateInfo.changelog?.firstOrNull()?.changes?.firstOrNull() ?: "")
                putString("update_channel", updateInfo.channel)
                putInt("update_version_code", updateInfo.versionCode)
                putString("update_release_notes", updateInfo.releaseNotes)
                putString("update_release_date", updateInfo.releaseDate)
                putString("update_min_supported_version", updateInfo.minimumSupportedVersion)
                putBoolean("update_force_update", updateInfo.forceUpdate)
            } else {
                putBoolean("has_update_available", false)
                remove("update_version")
                remove("update_url")
                remove("update_size")
                remove("update_checksum")
                remove("update_changelog")
                remove("update_channel")
                remove("update_version_code")
                remove("update_release_notes")
                remove("update_release_date")
                remove("update_min_supported_version")
                remove("update_force_update")
            }
            apply()
        }
    }

    fun checkForUpdates(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateCheckState = UpdateCheckState.Checking)

            val result = localUpdateManager.checkForUpdate(
                _uiState.value.selectedChannel,
                _uiState.value.currentVersion
            )

            val newState = when (result) {
                is UpdateCheckResult.UpdateAvailable -> UpdateCheckState.UpdateAvailable(result.updateInfo)
                is UpdateCheckResult.UpToDate -> UpdateCheckState.Idle
                is UpdateCheckResult.Error -> UpdateCheckState.Error(result.message)
            }

            _uiState.value = _uiState.value.copy(updateCheckState = newState)

            // Salva lo stato se c'è un aggiornamento disponibile
            if (result is UpdateCheckResult.UpdateAvailable) {
                // Se esiste un APK scaricato di versione inferiore, eliminalo
                maybeCleanupOldDownloadedApk(context, result.updateInfo.latestVersion)
                saveUpdateState(context, result.updateInfo)
                // Invia una notifica per informare l'utente
                UpdateNotifications.showUpdateAvailableNotification(
                    context = context,
                    version = result.updateInfo.latestVersion,
                    downloadUrl = result.updateInfo.downloadUrl,
                    releaseNotes = result.updateInfo.releaseNotes
                )
            } else if (result is UpdateCheckResult.UpToDate) {
                saveUpdateState(context, null)
                // Se l'app è aggiornata, rimuovi eventuale APK scaricato residuo che non serve
                maybeCleanupOldDownloadedApk(context, _uiState.value.currentVersion)
            }
        }
    }

    fun ignoreCurrentUpdate(context: Context) {
        _uiState.value = _uiState.value.copy(updateCheckState = UpdateCheckState.Idle)
        saveUpdateState(context, null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun downloadUpdate(context: Context, updateInfo: LocalUpdateManager.UpdateInfo) {
        // Context disponibile per eventuali operazioni future
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(downloadState = DownloadState.Downloading(0))

            val fileName = "update_${updateInfo.latestVersion}.apk"
            val result = downloadManager.downloadFile(
                url = updateInfo.downloadUrl,
                fileName = fileName,
                expectedSize = updateInfo.fileSize,
                expectedChecksum = updateInfo.checksum,
                callback = object : DownloadCallback {
                    override fun onProgress(state: DownloadManager.DownloadState) {
                        when (state) {
                            is DownloadManager.DownloadState.Downloading -> {
                                _uiState.value = _uiState.value.copy(
                                    downloadState = DownloadState.Downloading(state.progress)
                                )
                            }
                            is DownloadManager.DownloadState.Completed -> {
                                _uiState.value = _uiState.value.copy(
                                    downloadState = DownloadState.Completed(state.file)
                                )
                            }
                            is DownloadManager.DownloadState.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    downloadState = DownloadState.Error(state.message)
                                )
                            }
                            else -> {}
                        }
                    }

                    override fun onComplete(file: File) {
                        _uiState.value = _uiState.value.copy(
                            downloadState = DownloadState.Completed(file)
                        )
                        // Salva info del pacchetto scaricato
                        saveDownloadedApkInfo(context, file, updateInfo.latestVersion)
                        // Avvia installazione automaticamente
                        installUpdate(context, file)
                    }

                    override fun onError(message: String, canRetry: Boolean) {
                        _uiState.value = _uiState.value.copy(
                            downloadState = DownloadState.Error(message)
                        )
                    }
                }
            )

            when (result) {
                is DownloadResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        downloadState = DownloadState.Completed(result.file)
                    )
                    // Persisti anche in questo ramo
                    saveDownloadedApkInfo(context, result.file, updateInfo.latestVersion)
                }
                is DownloadResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        downloadState = DownloadState.Error(result.message)
                    )
                }
            }
        }
    }

    fun setSelectedChannel(context: Context, channel: ReleaseChannel) {
        localUpdateManager.setSelectedChannel(channel)
        _uiState.value = _uiState.value.copy(selectedChannel = channel)

        // Ricontrolla aggiornamenti per il nuovo canale
        checkForUpdates(context)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_update_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(isAutoUpdateEnabled = enabled)
    }

    fun setAutoDownloadEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_download_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(isAutoDownloadEnabled = enabled)
    }

    private fun getAutoUpdateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_update_enabled", true)
    }

    private fun getAutoDownloadEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_download_enabled", false)
    }

    fun retryDownload(context: Context, updateInfo: LocalUpdateManager.UpdateInfo) {
        downloadUpdate(context, updateInfo)
    }

    fun installUpdate(context: Context, apkFile: File) {

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    // Apri impostazioni per concedere il permesso
                    val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    // Lascia anche il file in stato completato; l'utente potrà riprovare l'installazione dal pulsante UI
                    return
                }
            }

            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                clipData = android.content.ClipData.newRawUri("APK", apkUri)
                addFlags(android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }

            // Concede esplicitamente il permesso a tutti i potential handler
            val pm = context.packageManager
            val resList = pm.queryIntentActivities(installIntent, 0)
            for (res in resList) {
                context.grantUriPermission(
                    res.activityInfo.packageName,
                    apkUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                downloadState = DownloadState.Error("Errore nell'installazione: ${e.message}")
            )
        }
    }

    fun clearDownloadError() {
        _uiState.value = _uiState.value.copy(downloadState = DownloadState.Idle)
    }

    fun clearUpdateError() {
        _uiState.value = _uiState.value.copy(updateCheckState = UpdateCheckState.Idle)
    }

    // --- Helpers per gestione APK scaricati ---
    private fun saveDownloadedApkInfo(context: Context, file: File, version: String) {
        val prefs = context.getSharedPreferences(PrefKeys.FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PrefKeys.DOWNLOADED_APK_PATH, file.absolutePath)
            .putString(PrefKeys.DOWNLOADED_APK_VERSION, version)
            .apply()
    }

    private fun maybeCleanupOldDownloadedApk(context: Context, newVersion: String) {
        val prefs = context.getSharedPreferences(PrefKeys.FILE, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(PrefKeys.DOWNLOADED_APK_PATH, null)
        val savedVersion = prefs.getString(PrefKeys.DOWNLOADED_APK_VERSION, null)
        if (!savedPath.isNullOrEmpty() && !savedVersion.isNullOrEmpty()) {
            val cmp = compareVersions(savedVersion, newVersion)
            if (cmp < 0) {
                // Trovata versione più recente -> elimina il vecchio APK
                try {
                    val old = File(savedPath)
                    if (old.exists()) old.delete()
                } catch (_: Exception) {}
                prefs.edit().remove(PrefKeys.DOWNLOADED_APK_PATH).remove(PrefKeys.DOWNLOADED_APK_VERSION).apply()
                // Resetta lo stato di download per riflettere la rimozione
                _uiState.value = _uiState.value.copy(downloadState = DownloadState.Idle)
            }
        }
    }

    // Confronto semplice tra versioni tipo x.y.z (riempie con zeri se mancano componenti)
    private fun compareVersions(a: String, b: String): Int {
        fun parse(v: String) = v.split('.').mapNotNull { it.toIntOrNull() }
        val av = parse(a)
        val bv = parse(b)
        val max = maxOf(av.size, bv.size)
        for (i in 0 until max) {
            val ai = if (i < av.size) av[i] else 0
            val bi = if (i < bv.size) bv[i] else 0
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }
}
