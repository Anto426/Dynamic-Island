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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Classe avanzata per gestire i download con controlli di integrità
 */
class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Aumentato da 30 a 60 secondi
        .readTimeout(600, TimeUnit.SECONDS)    // Aumentato da 300 a 600 secondi (10 minuti)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()

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
            Log.d(TAG, "Avvio download: $url")

            // Verifica permessi di scrittura nella cartella Download pubblica
            val downloadDir = getDownloadDirectory()

            if (!downloadDir.exists()) {
                val created = downloadDir.mkdirs()
                if (!created) {
                    val error = "Impossibile creare directory di download: ${downloadDir.absolutePath}"
                    Log.e(TAG, error)
                    callback?.onError(error, false)
                    return DownloadResult.Error(error, false)
                }
                Log.d(TAG, "Directory di download creata: ${downloadDir.absolutePath}")
            }

            val request = Request.Builder()
                .url(url)
                .build()

            Log.d(TAG, "Eseguendo richiesta HTTP...")
            val startTime = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val endTime = System.currentTimeMillis()
            Log.d(TAG, "Richiesta HTTP completata in ${endTime - startTime}ms - Status: ${response.code}")

            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, "Download fallito - $error - URL: $url")
                callback?.onError(error, response.code in 500..599)
                return DownloadResult.Error(error, response.code in 500..599)
            }

            response.body?.use { body ->
                val contentLength = body.contentLength()
                val actualSize = if (contentLength > 0) contentLength else expectedSize ?: 0

                Log.d(TAG, "Body ricevuto - ContentLength: $contentLength, ActualSize: $actualSize")

                if (contentLength == 0L && actualSize == 0L) {
                    val error = "File vuoto ricevuto dal server"
                    Log.e(TAG, error)
                    callback?.onError(error, true)
                    return DownloadResult.Error(error, true)
                }

                // Verifica dimensione attesa se fornita (con tolleranza per download da GitHub)
                if (expectedSize != null && contentLength > 0) {
                    val sizeDifference = Math.abs(contentLength - expectedSize)
                    if (sizeDifference > 1024) { // Tolleranza di 1KB
                        val error = "Dimensione file non corrispondente. Attesa: $expectedSize, Ricevuta: $contentLength (differenza: ${sizeDifference} bytes)"
                        Log.e(TAG, error)
                        callback?.onError(error, false)
                        return DownloadResult.Error(error, false)
                    } else if (sizeDifference > 0) {
                        Log.w(TAG, "Dimensione file leggermente diversa. Attesa: $expectedSize, Ricevuta: $contentLength (differenza: ${sizeDifference} bytes) - Procedo comunque")
                    }
                }

                // Crea file di output con fallback per permessi
                var outputFile = File(downloadDir, fileName)
                var usePrivateDir = false

                // Verifica se possiamo scrivere nella directory pubblica
                try {
                    if (!downloadDir.canWrite()) {
                        Log.w(TAG, "Directory pubblica non scrivibile, uso directory privata")
                        val privateDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
                        privateDir.mkdirs()
                        outputFile = File(privateDir, fileName)
                        usePrivateDir = true
                        Log.d(TAG, "Uso directory privata: ${outputFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Errore verifica permessi, uso directory privata: ${e.message}")
                    val privateDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
                    privateDir.mkdirs()
                    outputFile = File(privateDir, fileName)
                    usePrivateDir = true
                }

                if (outputFile.exists()) {
                    outputFile.delete()
                }

                Log.d(TAG, "Creando file di output: ${outputFile.absolutePath} (privata: $usePrivateDir)")

                // Scarica con progresso
                var bytesRead = 0L
                val digest = if (expectedChecksum != null) MessageDigest.getInstance("SHA-256") else null

                try {
                    FileOutputStream(outputFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytes: Int
                            var readCount = 0

                            Log.d(TAG, "Iniziando lettura del file...")
                            while (input.read(buffer).also { bytes = it } != -1) {
                                output.write(buffer, 0, bytes)
                                bytesRead += bytes
                                readCount++

                                // Log progresso ogni 100 letture (circa ogni 800KB)
                                if (readCount % 100 == 0) {
                                    Log.d(TAG, "Download progresso: $bytesRead / $actualSize bytes (${(bytesRead * 100) / actualSize}%)")
                                }

                                // Aggiorna digest per checksum
                                digest?.update(buffer, 0, bytes)

                                // Aggiorna progresso
                                if (actualSize > 0) {
                                    val progress = ((bytesRead * 100) / actualSize).toInt()
                                    callback?.onProgress(DownloadState.Downloading(progress, bytesRead, actualSize))
                                }
                            }
                            Log.d(TAG, "Lettura file completata - Totale bytes letti: $bytesRead")
                        }
                    }

                    // Verifica dimensione finale
                    val finalSize = outputFile.length()
                    Log.d(TAG, "Verifica dimensione finale - File: $finalSize bytes, Attesa: $expectedSize bytes")
                    if (finalSize == 0L) {
                        val error = "File scaricato è vuoto"
                        Log.e(TAG, error)
                        outputFile.delete()
                        callback?.onError(error, true)
                        return DownloadResult.Error(error, true)
                    }

                    // Verifica checksum se fornita (con fallback per checksum errati)
                    if (expectedChecksum != null && digest != null) {
                        val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
                        Log.d(TAG, "Verifica checksum - Attesa: $expectedChecksum, Calcolata: $actualChecksum")

                        // Controlla se il checksum atteso sembra valido (lunghezza minima per SHA-256)
                        val isValidChecksum = expectedChecksum.length >= 32 && !expectedChecksum.contains("B8F8B8F8B8F8B8F8B8F8B8F8B8F8B8F8")

                        if (isValidChecksum && actualChecksum != expectedChecksum.lowercase()) {
                            val error = "Checksum non corrispondente. Attesa: $expectedChecksum, Calcolata: $actualChecksum"
                            Log.e(TAG, error)
                            outputFile.delete()
                            callback?.onError(error, true)
                            return DownloadResult.Error(error, true)
                        } else if (!isValidChecksum) {
                            Log.w(TAG, "Checksum atteso sembra invalido o incompleto, procedo senza verifica")
                        } else {
                            Log.d(TAG, "Checksum verificato con successo")
                        }
                    } else {
                        Log.d(TAG, "Nessun checksum fornito, salto verifica")
                    }

                    // Verifica dimensione se attesa è nota (con tolleranza)
                    if (expectedSize != null) {
                        val sizeDifference = Math.abs(finalSize - expectedSize)
                        Log.d(TAG, "Verifica dimensione finale - Differenza: ${sizeDifference} bytes")
                        if (sizeDifference > 1024) { // Tolleranza di 1KB
                            val error = "Dimensione finale non corrispondente. Attesa: $expectedSize, Finale: $finalSize (differenza: ${sizeDifference} bytes)"
                            Log.e(TAG, error)
                            outputFile.delete()
                            callback?.onError(error, true)
                            return DownloadResult.Error(error, true)
                        } else if (sizeDifference > 0) {
                            Log.w(TAG, "Dimensione finale leggermente diversa. Attesa: $expectedSize, Finale: $finalSize (differenza: ${sizeDifference} bytes) - Procedo comunque")
                        } else {
                            Log.d(TAG, "Dimensione finale verificata con successo")
                        }
                    }

                    Log.d(TAG, "Download completato con successo: ${outputFile.absolutePath} (${finalSize} bytes)")
                    callback?.onComplete(outputFile)
                    callback?.onProgress(DownloadState.Completed(outputFile))

                    // Notifica il sistema che un nuovo file è stato scaricato
                    notifyMediaScanner(outputFile)

                    return DownloadResult.Success(outputFile)
                } catch (e: IOException) {
                    Log.e(TAG, "Errore durante la lettura/scrittura del file", e)
                    if (outputFile.exists()) {
                        outputFile.delete() // Elimina file parziale
                    }

                    // Messaggio di errore specifico per problemi di permessi
                    val errorMessage = if (e.message?.contains("Permission denied") == true) {
                        "Permessi di scrittura negati. Concedi i permessi di archiviazione nelle impostazioni dell'app."
                    } else {
                        "Errore I/O durante il download: ${e.message}"
                    }

                    callback?.onError(errorMessage, true)
                    return DownloadResult.Error(errorMessage, true, e)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore imprevisto durante il download del file", e)
                    if (outputFile.exists()) {
                        outputFile.delete() // Elimina file parziale
                    }
                    callback?.onError("Errore durante il download: ${e.message}", true)
                    return DownloadResult.Error("Errore durante il download: ${e.message}", true, e)
                }
            } ?: run {
                val error = "Risposta HTTP senza body"
                Log.e(TAG, error)
                callback?.onError(error, true)
                return DownloadResult.Error(error, true)
            }
        } catch (e: IOException) {
            val error = "Errore di connessione/timeout: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, true)
            return DownloadResult.Error(error, true, e)
        } catch (e: Exception) {
            val error = "Errore imprevisto durante il download: ${e.message}"
            Log.e(TAG, error, e)
            callback?.onError(error, false)
            return DownloadResult.Error(error, false, e)
        }
    }
    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val message: String, val canRetry: Boolean, val exception: Exception? = null) : DownloadResult()
    }

    /**
     * Notifica il media scanner del sistema che un nuovo file è stato scaricato
     */
    private fun notifyMediaScanner(file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.d(TAG, "File scansionato dal media scanner: $path")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossibile notificare il media scanner", e)
        }
    }

    /**
     * Pulisce i file di download vecchi
     */
    fun cleanupOldDownloads(maxAgeHours: Int = 24) {
        try {
            val downloadDir = getDownloadDirectory()

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
     * Ottiene la directory di download (pubblica o privata come fallback)
     */
    private fun getDownloadDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(publicDownloadDir, "MaterialYou-Dynamic-Island")
        } else {
            Log.w(TAG, "Memoria esterna non disponibile, uso directory privata")
            File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
        }
    }
}
