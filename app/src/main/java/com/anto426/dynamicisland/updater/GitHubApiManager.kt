package com.anto426.dynamicisland.updater

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Classe per gestire le API di GitHub e controllare i nuovi release
 */
class GitHubApiManager(
    private val owner: String = "Anto426",
    private val repo: String = "Dynamic-Island"
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    companion object {
        private const val TAG = "GitHubApiManager"
        private const val GITHUB_API_BASE = "https://api.github.com"
    }

    /**
     * Rappresenta un release di GitHub
     */
    data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("name") val name: String,
        @SerializedName("body") val body: String?,
        @SerializedName("published_at") val publishedAt: String,
        @SerializedName("assets") val assets: List<Asset>,
        @SerializedName("prerelease") val prerelease: Boolean = false
    )

    /**
     * Rappresenta un asset (file) di un release
     */
    data class Asset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String,
        @SerializedName("size") val size: Long
    )

    /**
     * Ottiene l'ultimo release stabile (non prerelease)
     */
    suspend fun getLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$GITHUB_API_BASE/repos/$owner/$repo/releases/latest")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Errore nella richiesta: ${response.code}")
                    return@withContext null
                }

                val json = response.body?.string()
                if (json.isNullOrEmpty()) {
                    Log.e(TAG, "Risposta vuota dal server")
                    return@withContext null
                }

                val release = gson.fromJson(json, GitHubRelease::class.java)
                Log.d(TAG, "Release trovato: ${release.tagName}")
                return@withContext release
            }
        } catch (e: IOException) {
            Log.e(TAG, "Errore di rete", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Errore generico", e)
            return@withContext null
        }
    }

    /**
     * Ottiene tutti i release (per confronto versioni)
     */
    suspend fun getAllReleases(): List<GitHubRelease>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$GITHUB_API_BASE/repos/$owner/$repo/releases")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Errore nella richiesta: ${response.code}")
                    return@withContext null
                }

                val json = response.body?.string()
                if (json.isNullOrEmpty()) {
                    Log.e(TAG, "Risposta vuota dal server")
                    return@withContext null
                }

                val releases = gson.fromJson(json, Array<GitHubRelease>::class.java).toList()
                Log.d(TAG, "Trovati ${releases.size} release")
                return@withContext releases
            }
        } catch (e: IOException) {
            Log.e(TAG, "Errore di rete", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Errore generico", e)
            return@withContext null
        }
    }

    /**
     * Trova l'asset APK nel release
     */
    fun findApkAsset(release: GitHubRelease): Asset? {
        return release.assets.find { asset ->
            asset.name.endsWith(".apk", ignoreCase = true)
        }
    }

    /**
     * Confronta due versioni (semplificato)
     * Restituisce true se version1 > version2
     */
    fun isVersionNewer(version1: String, version2: String): Boolean {
        try {
            val v1 = version1.removePrefix("v").split(".").map { it.toInt() }
            val v2 = version2.removePrefix("v").split(".").map { it.toInt() }

            for (i in 0 until maxOf(v1.size, v2.size)) {
                val part1 = v1.getOrElse(i) { 0 }
                val part2 = v2.getOrElse(i) { 0 }

                if (part1 > part2) return true
                if (part1 < part2) return false
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel confronto versioni: $version1 vs $version2", e)
            return false
        }
    }
}
