package com.anto426.dynamicisland.updater

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Classe di utilità per gestire gli aggiornamenti dell'app
 * Ora utilizza il nuovo sistema basato su file JSON locali
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val PREF_LAST_CHECK = "last_update_check"
        private const val PREF_IGNORED_VERSION = "ignored_version"
    }

    private val _updateState = MutableLiveData<UpdateState>()
    val updateState: LiveData<UpdateState> = _updateState

    private val workManager = WorkManager.getInstance(context)
    private val preferences = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    // Nuovi manager
    private val localUpdateManager = LocalUpdateManager(context)
    private val downloadManager = DownloadManager(context)

    /**
     * Stati possibili dell'aggiornamento
     */
    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class Available(val version: String, val downloadUrl: String, val releaseNotes: String?) : UpdateState()
        data class Downloading(val progress: Int) : UpdateState()
        object Downloaded : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    /**
     * Ottiene la versione attuale dell'app
     */
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Errore nell'ottenere la versione dell'app", e)
            "1.0.0"
        }
    }

    /**
     * Avvia il controllo periodico degli aggiornamenti
     */
    fun startPeriodicUpdateCheck() {
        val currentVersion = getCurrentVersion()
        UpdateCheckWorker.schedulePeriodicCheck(context, currentVersion)
        Log.d(TAG, "Controllo aggiornamenti periodico avviato")
    }

    /**
     * Forza un controllo immediato degli aggiornamenti
     */
    fun checkForUpdatesNow() {
        _updateState.value = UpdateState.Checking

        val currentVersion = getCurrentVersion()
        val selectedChannel = localUpdateManager.getSelectedChannel()

        // Usa il nuovo sistema di controllo
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = localUpdateManager.checkForUpdate(selectedChannel, currentVersion)

                when (result) {
                    is LocalUpdateManager.UpdateCheckResult.UpdateAvailable -> {
                        val updateInfo = result.updateInfo
                        // Controlla se la versione è stata ignorata
                        if (preferences.getString(PREF_IGNORED_VERSION, "") != updateInfo.latestVersion) {
                            _updateState.postValue(
                                UpdateState.Available(
                                    version = updateInfo.latestVersion,
                                    downloadUrl = updateInfo.downloadUrl,
                                    releaseNotes = updateInfo.releaseNotes
                                )
                            )

                            // Notifica aggiornamento disponibile
                            UpdateNotifications.showUpdateAvailableNotification(
                                context = context,
                                version = updateInfo.latestVersion,
                                downloadUrl = updateInfo.downloadUrl,
                                releaseNotes = updateInfo.releaseNotes
                            )
                        } else {
                            _updateState.postValue(UpdateState.Idle)
                        }
                    }
                    is LocalUpdateManager.UpdateCheckResult.UpToDate -> {
                        _updateState.postValue(UpdateState.Idle)
                    }
                    is LocalUpdateManager.UpdateCheckResult.Error -> {
                        _updateState.postValue(UpdateState.Error(result.message))
                    }
                }

                // Aggiorna il timestamp dell'ultimo controllo
                updateLastCheckTime()

            } catch (e: Exception) {
                Log.e(TAG, "Errore durante il controllo aggiornamenti", e)
                _updateState.postValue(UpdateState.Error("Errore durante il controllo: ${e.message}"))
            }
        }
    }

    /**
     * Controlla gli aggiornamenti all'avvio dell'app (solo se necessario)
     * Rispetta l'intervallo minimo tra controlli per evitare richieste eccessive
     */
    fun checkForUpdatesOnStartup() {
        if (shouldCheckForUpdatesOnStartup()) {
            Log.d(TAG, "Controllo aggiornamenti all'avvio dell'app")
            checkForUpdatesNow()
        } else {
            Log.d(TAG, "Controllo aggiornamenti saltato (troppo recente)")
        }
    }

    /**
     * Determina se è necessario controllare gli aggiornamenti all'avvio
     * Controlla ogni 6 ore per evitare richieste eccessive
     */
    private fun shouldCheckForUpdatesOnStartup(): Boolean {
        val lastCheckTime = preferences.getLong(PREF_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()
        val sixHoursInMillis = 6 * 60 * 60 * 1000L // 6 ore

        return (currentTime - lastCheckTime) > sixHoursInMillis
    }
    fun downloadUpdate(version: String, downloadUrl: String) {
        _updateState.value = UpdateState.Downloading(0)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fileName = "update_$version.apk"
                val result = downloadManager.downloadFile(
                    url = downloadUrl,
                    fileName = fileName,
                    callback = object : DownloadManager.DownloadCallback {
                        override fun onProgress(state: DownloadManager.DownloadState) {
                            when (state) {
                                is DownloadManager.DownloadState.Downloading -> {
                                    _updateState.postValue(UpdateState.Downloading(state.progress))
                                }
                                is DownloadManager.DownloadState.Completed -> {
                                    _updateState.postValue(UpdateState.Downloaded)
                                }
                                is DownloadManager.DownloadState.Error -> {
                                    _updateState.postValue(UpdateState.Error(state.message))
                                }
                                else -> {}
                            }
                        }

                        override fun onComplete(file: java.io.File) {
                            _updateState.postValue(UpdateState.Downloaded)
                        }

                        override fun onError(message: String, canRetry: Boolean) {
                            _updateState.postValue(UpdateState.Error(message))
                        }
                    }
                )

                when (result) {
                    is DownloadManager.DownloadResult.Success -> {
                        _updateState.postValue(UpdateState.Downloaded)
                    }
                    is DownloadManager.DownloadResult.Error -> {
                        _updateState.postValue(UpdateState.Error(result.message))
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Errore durante il download", e)
                _updateState.postValue(UpdateState.Error("Errore durante il download: ${e.message}"))
            }
        }
    }

    /**
     * Ignora questa versione dell'aggiornamento
     */
    fun ignoreUpdate(version: String) {
        preferences.edit().putString(PREF_IGNORED_VERSION, version).apply()
        _updateState.value = UpdateState.Idle
    }

    /**
     * Reimposta la versione ignorata (per testing)
     */
    fun resetIgnoredVersion() {
        preferences.edit().remove(PREF_IGNORED_VERSION).apply()
    }

    /**
     * Verifica se è passato abbastanza tempo dall'ultimo controllo
     */
    fun shouldCheckForUpdates(): Boolean {
        val lastCheck = preferences.getLong(PREF_LAST_CHECK, 0)
        val now = System.currentTimeMillis()
        val sixHoursInMillis = 6 * 60 * 60 * 1000L // 6 ore

        return (now - lastCheck) > sixHoursInMillis
    }

    /**
     * Aggiorna il timestamp dell'ultimo controllo
     */
    fun updateLastCheckTime() {
        preferences.edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    /**
     * Controlla se il controllo automatico è abilitato
     */
    fun isAutoUpdateEnabled(): Boolean {
        return preferences.getBoolean("auto_update_enabled", true)
    }

    /**
     * Abilita/disabilita il controllo automatico degli aggiornamenti
     */
    fun setAutoUpdateEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("auto_update_enabled", enabled).apply()

        if (enabled) {
            startPeriodicUpdateCheck()
        } else {
            workManager.cancelUniqueWork("update_check")
        }
    }

    /**
     * Ottiene il canale di rilascio selezionato
     */
    fun getSelectedChannel(): LocalUpdateManager.ReleaseChannel {
        return localUpdateManager.getSelectedChannel()
    }

    /**
     * Imposta il canale di rilascio selezionato
     */
    fun setSelectedChannel(channel: LocalUpdateManager.ReleaseChannel) {
        localUpdateManager.setSelectedChannel(channel)
    }

    /**
     * Controlla se il download automatico è abilitato
     */
    fun isAutoDownloadEnabled(): Boolean {
        return preferences.getBoolean("auto_download_enabled", false)
    }

    /**
     * Abilita/disabilita il download automatico degli aggiornamenti
     */
    fun setAutoDownloadEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("auto_download_enabled", enabled).apply()
    }

    /**
     * Controlla se l'app è aggiornata (versione attuale vs versione dal canale selezionato)
     */
    suspend fun isAppUpToDate(): Boolean {
        return try {
            val selectedChannel = localUpdateManager.getSelectedChannel()
            val currentVersion = getCurrentVersion()

            val result = localUpdateManager.checkForUpdate(selectedChannel, currentVersion)

            return when (result) {
                is LocalUpdateManager.UpdateCheckResult.UpdateAvailable -> false
                is LocalUpdateManager.UpdateCheckResult.UpToDate -> true
                is LocalUpdateManager.UpdateCheckResult.Error -> true // Se errore, assumiamo aggiornata
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel controllo versione", e)
            return true
        }
    }
}
