package com.anto426.dynamicisland.updater

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Classe per gestire gli aggiornamenti basati su file JSON da GitHub
 */
class LocalUpdateManager(private val context: Context) {

    private val gson = Gson()
    private val client = OkHttpClient()

    companion object {
        private const val TAG = "LocalUpdateManager"
    }

    /**
     * Canali di rilascio disponibili
     */
    enum class ReleaseChannel {
        STABLE, BETA, ALPHA
    }

    /**
     * Rappresenta un aggiornamento dal file JSON
     */
    data class UpdateInfo(
        @SerializedName("channel") val channel: String?,
        @SerializedName("latest_version") val latestVersion: String,
        @SerializedName("version_code") val versionCode: Int,
        @SerializedName("download_url") val downloadUrl: String,
        @SerializedName("release_notes") val releaseNotes: String?,
        @SerializedName("release_date") val releaseDate: String?,
        @SerializedName("minimum_supported_version") val minimumSupportedVersion: String?,
        @SerializedName("force_update") val forceUpdate: Boolean,
        @SerializedName("checksum") val checksum: String?,
        @SerializedName("file_size") val fileSize: Long?,
        @SerializedName("changelog") val changelog: List<ChangelogEntry>?
    )

    /**
     * Voce del changelog
     */
    data class ChangelogEntry(
        @SerializedName("version") val version: String,
        @SerializedName("date") val date: String,
        @SerializedName("changes") val changes: List<String>
    )

    /**
     * Ottiene le informazioni di aggiornamento per un canale specifico
     * Carica esclusivamente da GitHub
     */
    suspend fun getUpdateInfo(channel: ReleaseChannel): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val fileName = when (channel) {
                ReleaseChannel.STABLE -> "stable.json"
                ReleaseChannel.BETA -> "beta.json"
                ReleaseChannel.ALPHA -> "alpha.json"
            }

            // Carica esclusivamente da GitHub
            val updateInfo = loadUpdateInfoFromUrl(fileName)

            if (updateInfo == null) {
                Log.w(TAG, "Impossibile caricare informazioni aggiornamento da GitHub per canale: $channel")
                return@withContext null
            }

            Log.d(TAG, "Informazioni aggiornamento caricate da GitHub per ${updateInfo.channel}: ${updateInfo.latestVersion}")
            return@withContext updateInfo

        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento informazioni aggiornamento da GitHub", e)
            return@withContext null
        }
    }

    /**
     * Carica le informazioni da URL remoto (GitHub)
     */
    private suspend fun loadUpdateInfoFromUrl(fileName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            // URL base del repository GitHub
            val baseUrl = "https://raw.githubusercontent.com/Anto426/Dynamic-Island/main/release"
            val url = "$baseUrl/$fileName"

            Log.d(TAG, "Tentativo di caricamento da: $url")

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Errore HTTP ${response.code} nel caricamento da GitHub: $url")
                    return@withContext null
                }

                val json = response.body.string()
                if (json.isNullOrEmpty()) {
                    Log.e(TAG, "Risposta vuota da GitHub per: $url")
                    return@withContext null
                }

                val updateInfo = gson.fromJson(json, UpdateInfo::class.java)

                Log.d(TAG, "Informazioni caricate con successo da GitHub: $url")
                updateInfo
            }
        } catch (e: IOException) {
            Log.e(TAG, "Errore di rete nel caricamento da GitHub: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Errore generico nel caricamento da GitHub: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se è disponibile un aggiornamento
     */
    suspend fun checkForUpdate(channel: ReleaseChannel, currentVersion: String): UpdateCheckResult {
        return try {
            val updateInfo = getUpdateInfo(channel)

            if (updateInfo == null) {
                return UpdateCheckResult.Error("Impossibile caricare informazioni aggiornamento")
            }

            val isNewer = isVersionNewer(updateInfo.latestVersion, currentVersion)

            if (isNewer) {
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore nel controllo aggiornamenti", e)
            UpdateCheckResult.Error("Errore durante il controllo: ${e.message}")
        }
    }

    /**
     * Risultato del controllo aggiornamenti
     */
    sealed class UpdateCheckResult {
        data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
        object UpToDate : UpdateCheckResult()
        data class Error(val message: String) : UpdateCheckResult()
    }

    /**
     * Confronta due versioni
     */
    private fun isVersionNewer(version1: String, version2: String): Boolean {
        try {
            val v1 = version1.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val v2 = version2.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

            // Gestione versioni beta/alpha
            val v1Clean = v1.filter { it >= 0 }
            val v2Clean = v2.filter { it >= 0 }

            for (i in 0 until maxOf(v1Clean.size, v2Clean.size)) {
                val part1 = v1Clean.getOrElse(i) { 0 }
                val part2 = v2Clean.getOrElse(i) { 0 }

                if (part1 > part2) return true
                if (part1 < part2) return false
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel confronto versioni: $version1 vs $version2", e)
            return false
        }
    }

    /**
     * Ottiene il canale di rilascio selezionato dall'utente
     */
    fun getSelectedChannel(): ReleaseChannel {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val channelString = prefs.getString("selected_channel", "stable") ?: "stable"

        return when (channelString) {
            "beta" -> ReleaseChannel.BETA
            "alpha" -> ReleaseChannel.ALPHA
            else -> ReleaseChannel.STABLE
        }
    }

    /**
     * Imposta il canale di rilascio selezionato
     */
    fun setSelectedChannel(channel: ReleaseChannel) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val channelString = when (channel) {
            ReleaseChannel.STABLE -> "stable"
            ReleaseChannel.BETA -> "beta"
            ReleaseChannel.ALPHA -> "alpha"
        }
        prefs.edit().putString("selected_channel", channelString).apply()
    }
}
