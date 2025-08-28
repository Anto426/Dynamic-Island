package com.anto426.dynamicisland.updater

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

// Import locali per le classi del sistema di aggiornamenti
import com.anto426.dynamicisland.updater.UpdateCheckWorker
import com.anto426.dynamicisland.updater.UpdateDownloadService
import com.anto426.dynamicisland.updater.GitHubApiManager

/**
 * Classe di utilità per gestire gli aggiornamenti dell'app
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
        UpdateCheckWorker.checkNow(context, currentVersion)

        // Osserva il risultato del lavoro
        workManager.getWorkInfosForUniqueWorkLiveData("update_check")
            .observeForever { workInfos ->
                workInfos?.firstOrNull()?.let { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val hasUpdate = workInfo.outputData.getBoolean(UpdateCheckWorker.KEY_CHECK_RESULT, false)
                            if (hasUpdate) {
                                val newVersion = workInfo.outputData.getString(UpdateCheckWorker.KEY_NEW_VERSION)
                                val downloadUrl = workInfo.outputData.getString(UpdateCheckWorker.KEY_DOWNLOAD_URL)

                                if (newVersion != null && downloadUrl != null) {
                                    // Controlla se la versione è stata ignorata
                                    if (preferences.getString(PREF_IGNORED_VERSION, "") != newVersion) {
                                        _updateState.value = UpdateState.Available(
                                            version = newVersion,
                                            downloadUrl = downloadUrl,
                                            releaseNotes = null // Potremmo aggiungere le note di rilascio più avanti
                                        )
                                    } else {
                                        _updateState.value = UpdateState.Idle
                                    }
                                }
                            } else {
                                _updateState.value = UpdateState.Idle
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            _updateState.value = UpdateState.Error("Controllo aggiornamenti fallito")
                        }
                        else -> {
                            // Stato intermedio, non cambiare lo stato
                        }
                    }
                }
            }
    }

    /**
     * Scarica l'aggiornamento
     */
    fun downloadUpdate(version: String, downloadUrl: String) {
        _updateState.value = UpdateState.Downloading(0)
        UpdateDownloadService.startDownload(context, downloadUrl, version)
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
     * Controlla se l'app è aggiornata (versione attuale vs versione su GitHub)
     */
    suspend fun isAppUpToDate(): Boolean {
        return try {
            val apiManager = GitHubApiManager()
            val latestRelease = apiManager.getLatestRelease()

            if (latestRelease != null) {
                val currentVersion = getCurrentVersion()
                return !apiManager.isVersionNewer(latestRelease.tagName, currentVersion)
            } else {
                return true // Se non riusciamo a controllare, assumiamo che sia aggiornata
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel controllo versione", e)
            return true
        }
    }
}
