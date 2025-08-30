package com.anto426.dynamicisland.updater

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.anto426.dynamicisland.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Servizio per scaricare e installare gli aggiornamenti APK
 */
class UpdateDownloadService : IntentService("UpdateDownloadService") {

    companion object {
        private const val TAG = "UpdateDownloadService"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1002

        // Azioni del servizio
        const val ACTION_START_DOWNLOAD = "com.anto426.dynamicisland.START_DOWNLOAD"
        const val ACTION_INSTALL_APK = "com.anto426.dynamicisland.INSTALL_APK"

        // Extra per gli intent
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_VERSION = "version"
        const val EXTRA_APK_PATH = "apk_path"

        /**
         * Avvia il download di un aggiornamento
         */
        fun startDownload(context: Context, downloadUrl: String, version: String) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_VERSION, version)
            }
            context.startService(intent)
        }

        /**
         * Avvia l'installazione di un APK
         */
        fun startInstall(context: Context, apkPath: String) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_INSTALL_APK
                putExtra(EXTRA_APK_PATH, apkPath)
            }
            context.startService(intent)
        }
    }

    private val client = OkHttpClient()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createInitialNotification())
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL)
                val version = intent.getStringExtra(EXTRA_VERSION)

                if (downloadUrl != null && version != null) {
                    downloadAndInstallApk(downloadUrl, version)
                }
            }
            ACTION_INSTALL_APK -> {
                val apkPath = intent.getStringExtra(EXTRA_APK_PATH)
                if (apkPath != null) {
                    installApk(apkPath)
                }
            }
        }
    }

    /**
     * Scarica e installa l'APK
     */
    private fun downloadAndInstallApk(downloadUrl: String, version: String) {
        try {
            Log.d(TAG, "Inizio download APK: $downloadUrl")

            // Crea la directory per gli aggiornamenti
            val updatesDir = File(getExternalFilesDir(null), "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val apkFile = File(updatesDir, "update_$version.apk")

            // Scarica il file
            downloadFile(downloadUrl, apkFile)

            if (apkFile.exists() && apkFile.length() > 0) {
                Log.d(TAG, "Download completato: ${apkFile.absolutePath}")
                updateNotification("Download completato", "Installazione in corso...", 100)

                // Installa l'APK
                installApk(apkFile.absolutePath)
            } else {
                Log.e(TAG, "File APK non valido o vuoto")
                updateNotification("Errore", "Download fallito", 0)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante download/installazione", e)
            updateNotification("Errore", "Impossibile scaricare l'aggiornamento", 0)
        }
    }

    /**
     * Scarica un file da URL
     */
    private fun downloadFile(url: String, outputFile: File) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download fallito: ${response.code}")
            }

            val body = response.body ?: throw IOException("Risposta vuota")

            val contentLength = body.contentLength()
            var bytesRead = 0L

            FileOutputStream(outputFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes

                        // Aggiorna la notifica con il progresso
                        if (contentLength > 0) {
                            val progress = ((bytesRead * 100) / contentLength).toInt()
                            updateNotification(
                                "Scaricamento...",
                                "${bytesRead / 1024 / 1024}MB / ${contentLength / 1024 / 1024}MB",
                                progress
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Installa l'APK scaricato
     */
    private fun installApk(apkPath: String) {
        try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                Log.e(TAG, "File APK non trovato: $apkPath")
                return
            }

            val apkUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Per Android 8.0+ potrebbe essere necessario un permesso aggiuntivo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    Log.w(TAG, "Permesso di installazione non concesso")
                    // Mostra una notifica per richiedere il permesso
                    showInstallPermissionNotification()
                    return
                }
            }

            startActivity(installIntent)
            updateNotification("Installazione", "Apertura installer APK...", 100)

            Log.d(TAG, "Installazione APK avviata")

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'installazione", e)
            updateNotification("Errore", "Impossibile installare l'aggiornamento", 0)
        }
    }

    /**
     * Mostra notifica per richiedere il permesso di installazione
     */
    private fun showInstallPermissionNotification() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:$packageName")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.permission_required_title))
            .setContentText(getString(R.string.permission_required_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Crea la notifica iniziale
     */
    private fun createInitialNotification(): Notification {
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.update_notification_title))
            .setContentText(getString(R.string.update_preparing_download))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        return notificationBuilder.build()
    }

    /**
     * Aggiorna la notifica con progresso
     */
    private fun updateNotification(title: String, text: String, progress: Int) {
        notificationBuilder
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, progress, progress == 0)

        if (progress == 100) {
            notificationBuilder.setOngoing(false)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Crea il canale di notifica
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Aggiornamenti",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.update_download_channel_description)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}
