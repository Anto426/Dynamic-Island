package com.anto426.dynamicisland.updater

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Classe avanzata per gestire i download con controlli di integrità
 */
class DownloadManager(private val context: Context) {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "DownloadManager"
        private const val DOWNLOAD_DIR = "downloads"
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 3
    }

    /**
     * Stato del download
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String, val canRetry: Boolean = true) : DownloadState()
    }

    /**
     * Callback per il progresso del download
     */
    interface DownloadCallback {
        fun onProgress(state: DownloadState)
        fun onComplete(file: File)
        fun onError(message: String, canRetry: Boolean)
    }

    /**
     * Scarica un file con controlli avanzati
     */
    suspend fun downloadFile(
        url: String,
        fileName: String,
        expectedSize: Long? = null,
        expectedChecksum: String? = null,
        callback: DownloadCallback? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        // Tentativi multipli
        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "Tentativo $attempt di download: $url")

                callback?.onProgress(DownloadState.Downloading(0, 0, 0))

                val result = performDownload(url, fileName, expectedSize, expectedChecksum, callback)

                if (result is DownloadResult.Success) {
                    Log.d(TAG, "Download completato con successo al tentativo $attempt")
                    return@withContext result
                } else if (result is DownloadResult.Error && !result.canRetry) {
                    // Errore non recuperabile
                    return@withContext result
                }

                lastError = (result as? DownloadResult.Error)?.exception

            } catch (e: Exception) {
                Log.e(TAG, "Errore tentativo $attempt: ${e.message}", e)
                lastError = e
            }

            // Pausa tra tentativi
            if (attempt < MAX_RETRIES) {
                kotlinx.coroutines.delay(2000L * attempt)
            }
        }

        val errorMessage = "Download fallito dopo $MAX_RETRIES tentativi. Ultimo errore: ${lastError?.message}"
        Log.e(TAG, errorMessage)
        callback?.onError(errorMessage, false)
        return@withContext DownloadResult.Error(errorMessage, false, lastError)
    }

    /**
     * Esegue il download effettivo
     */
    private suspend fun performDownload(
        url: String,
        fileName: String,
        expectedSize: Long?,
        expectedChecksum: String?,
        callback: DownloadCallback?
    ): DownloadResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}: ${response.message}"
                callback?.onError(error, response.code in 500..599)
                return DownloadResult.Error(error, response.code in 500..599)
            }

            val body = response.body ?: run {
                val error = "Risposta vuota dal server"
                callback?.onError(error, true)
                return DownloadResult.Error(error, true)
            }

            val contentLength = body.contentLength()
            val actualSize = if (contentLength > 0) contentLength else expectedSize ?: 0

            // Verifica dimensione attesa se fornita
            if (expectedSize != null && contentLength > 0 && contentLength != expectedSize) {
                val error = "Dimensione file non corrispondente. Attesa: $expectedSize, Ricevuta: $contentLength"
                callback?.onError(error, false)
                return DownloadResult.Error(error, false)
            }

            // Crea directory e file
            val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val outputFile = File(downloadDir, fileName)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Scarica con progresso
            var bytesRead = 0L
            val digest = if (expectedChecksum != null) MessageDigest.getInstance("SHA-256") else null

            FileOutputStream(outputFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes

                        // Aggiorna digest per checksum
                        digest?.update(buffer, 0, bytes)

                        // Aggiorna progresso
                        if (actualSize > 0) {
                            val progress = ((bytesRead * 100) / actualSize).toInt()
                            callback?.onProgress(DownloadState.Downloading(progress, bytesRead, actualSize))
                        }
                    }
                }
            }

            // Verifica dimensione finale
            if (outputFile.length() == 0L) {
                val error = "File scaricato è vuoto"
                outputFile.delete()
                callback?.onError(error, true)
                return DownloadResult.Error(error, true)
            }

            // Verifica checksum se fornita
            if (expectedChecksum != null && digest != null) {
                val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
                if (actualChecksum != expectedChecksum) {
                    val error = "Checksum non corrispondente. Attesa: $expectedChecksum, Calcolata: $actualChecksum"
                    outputFile.delete()
                    callback?.onError(error, true)
                    return DownloadResult.Error(error, true)
                }
            }

            // Verifica dimensione se attesa è nota
            if (expectedSize != null && outputFile.length() != expectedSize) {
                val error = "Dimensione finale non corrispondente. Attesa: $expectedSize, Finale: ${outputFile.length()}"
                outputFile.delete()
                callback?.onError(error, true)
                return DownloadResult.Error(error, true)
            }

            Log.d(TAG, "Download completato: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            callback?.onComplete(outputFile)
            callback?.onProgress(DownloadState.Completed(outputFile))

            DownloadResult.Success(outputFile)

        } catch (e: IOException) {
            val error = "Errore di connessione: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, true)
            DownloadResult.Error(error, true, e)
        } catch (e: Exception) {
            val error = "Errore imprevisto: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, false)
            DownloadResult.Error(error, false, e)
        }
    }

    /**
     * Risultato del download
     */
    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val message: String, val canRetry: Boolean, val exception: Exception? = null) : DownloadResult()
    }

    /**
     * Verifica se un file è già scaricato e valido
     */
    fun isFileValid(file: File, expectedChecksum: String? = null, expectedSize: Long? = null): Boolean {
        if (!file.exists() || file.length() == 0L) {
            return false
        }

        // Verifica dimensione
        if (expectedSize != null && file.length() != expectedSize) {
            return false
        }

        // Verifica checksum
        if (expectedChecksum != null) {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val checksum = file.inputStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        digest.update(buffer, 0, bytes)
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
                checksum == expectedChecksum
            } catch (e: Exception) {
                Log.e(TAG, "Errore nella verifica checksum", e)
                false
            }
        }

        return true
    }

    /**
     * Pulisce i file di download vecchi
     */
    fun cleanupOldDownloads(maxAgeHours: Int = 24) {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
            if (!downloadDir.exists()) return

            val maxAgeMillis = maxAgeHours * 60 * 60 * 1000L
            val now = System.currentTimeMillis()

            downloadDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis) {
                    if (file.delete()) {
                        Log.d(TAG, "File vecchio eliminato: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella pulizia downloads", e)
        }
    }
}
