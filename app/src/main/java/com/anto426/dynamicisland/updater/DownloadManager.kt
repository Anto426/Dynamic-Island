package com.anto426.dynamicisland.updater

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "DownloadManager"
        private const val DOWNLOAD_DIR = "downloads"
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 3
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String, val canRetry: Boolean = true) : DownloadState()
    }

    interface DownloadCallback {
        fun onProgress(state: DownloadState)
        fun onComplete(file: File)
        fun onError(message: String, canRetry: Boolean)
    }

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val message: String, val canRetry: Boolean, val exception: Exception? = null) : DownloadResult()
    }

    suspend fun downloadFile(
        url: String,
        fileName: String,
        callback: DownloadCallback? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "Tentativo $attempt di download: $url")
                callback?.onProgress(DownloadState.Downloading(0, 0, 0))

                val result = performDownload(url, fileName, callback)
                when (result) {
                    is DownloadResult.Success -> return@withContext result
                    is DownloadResult.Error -> if (!result.canRetry) return@withContext result
                }
                lastError = (result as? DownloadResult.Error)?.exception
            } catch (e: Exception) {
                Log.e(TAG, "Errore tentativo $attempt: ${e.message}", e)
                lastError = e
            }
            if (attempt < MAX_RETRIES) kotlinx.coroutines.delay(2000L * attempt)
        }

        val errorMessage = "Download fallito dopo $MAX_RETRIES tentativi. Ultimo errore: ${lastError?.message}"
        Log.e(TAG, errorMessage)
        callback?.onError(errorMessage, false)
        DownloadResult.Error(errorMessage, false, lastError)
    }

    private suspend fun performDownload(
        url: String,
        fileName: String,
        callback: DownloadCallback?
    ): DownloadResult {
        return try {
            val downloadDir = getDownloadDirectory()
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                val error = "Impossibile creare directory di download: ${downloadDir.absolutePath}"
                Log.e(TAG, error)
                callback?.onError(error, false)
                return DownloadResult.Error(error, false)
            }

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, error)
                callback?.onError(error, response.code in 500..599)
                return DownloadResult.Error(error, response.code in 500..599)
            }

            val body = response.body ?: return DownloadResult.Error("Risposta vuota", true)
            body.use { responseBody ->
                val total = responseBody.contentLength()

                var outputFile = File(downloadDir, fileName)
                var usePrivateDir = false

                val outputStream = try {
                    if (outputFile.exists()) outputFile.delete()
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile)
                } catch (e: Exception) {
                    // Fallback su directory privata dell'app
                    val basePrivate = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.getExternalFilesDir(null)
                    val privateDir = if (basePrivate != null) File(basePrivate, "MaterialYou-Dynamic-Island") else File(context.filesDir, DOWNLOAD_DIR)
                    if (!privateDir.exists()) privateDir.mkdirs()
                    outputFile = File(privateDir, fileName)
                    usePrivateDir = true
                    if (outputFile.exists()) outputFile.delete()
                    FileOutputStream(outputFile)
                }

                Log.d(TAG, "Scrivo su: ${outputFile.absolutePath} (privata: $usePrivateDir)")

                var bytesRead = 0L
                outputStream.use { out ->
                    responseBody.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        var ticks = 0
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesRead += read
                            ticks++
                            if (total > 0) {
                                if (ticks % 100 == 0) {
                                    Log.d(TAG, "Download progresso: $bytesRead / $total bytes (${(bytesRead * 100) / total}%)")
                                }
                                val progress = ((bytesRead * 100) / total).toInt()
                                callback?.onProgress(DownloadState.Downloading(progress, bytesRead, total))
                            }
                        }
                    }
                }

                val finalSize = outputFile.length()
                if (finalSize == 0L) {
                    val error = "File scaricato è vuoto"
                    if (outputFile.exists()) outputFile.delete()
                    callback?.onError(error, true)
                    return DownloadResult.Error(error, true)
                }

                notifyMediaScanner(outputFile)
                callback?.onComplete(outputFile)
                callback?.onProgress(DownloadState.Completed(outputFile))
                DownloadResult.Success(outputFile)
            }
        } catch (e: IOException) {
            val error = "Errore di connessione/timeout: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, true)
            DownloadResult.Error(error, true, e)
        } catch (e: Exception) {
            val error = "Errore imprevisto durante il download: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, false)
            DownloadResult.Error(error, false, e)
        }
    }

    fun cleanupOldDownloads(maxAgeHours: Int = 24) {
        try {
            val downloadDir = getDownloadDirectory()
            if (!downloadDir.exists()) return
            val maxAgeMillis = maxAgeHours * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            downloadDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis) {
                    if (file.delete()) Log.d(TAG, "File vecchio eliminato: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella pulizia downloads", e)
        }
    }

    fun isFileValid(file: File): Boolean = file.exists() && file.length() > 0L

    private fun getDownloadDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(publicDownloadDir, "MaterialYou-Dynamic-Island")
        } else {
            Log.w(TAG, "Memoria esterna non disponibile, uso directory privata")
            File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
        }
    }

    private fun notifyMediaScanner(file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, _ -> Log.d(TAG, "File scansionato dal media scanner: $path") }
        } catch (e: Exception) {
            Log.w(TAG, "Impossibile notificare il media scanner", e)
        }
    }
}
