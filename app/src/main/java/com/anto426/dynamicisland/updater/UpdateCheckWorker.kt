package com.anto426.dynamicisland.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.anto426.dynamicisland.MainActivity
import com.anto426.dynamicisland.R
import com.anto426.dynamicisland.updater.GitHubApiManager.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager per controllare periodicamente gli aggiornamenti da GitHub
 */
class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1001

        // Chiavi per i dati di input/output
        const val KEY_CURRENT_VERSION = "current_version"
        const val KEY_CHECK_RESULT = "check_result"
        const val KEY_NEW_VERSION = "new_version"
        const val KEY_DOWNLOAD_URL = "download_url"

        /**
         * Pianifica un controllo aggiornamenti periodico
         */
        fun schedulePeriodicCheck(context: Context, currentVersion: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val inputData = workDataOf(KEY_CURRENT_VERSION to currentVersion)

            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                6, TimeUnit.HOURS, // Controlla ogni 6 ore
                1, TimeUnit.HOURS  // Flessibilità di 1 ora
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(30, TimeUnit.MINUTES) // Primo controllo dopo 30 minuti
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d(TAG, "Controllo aggiornamenti pianificato")
        }

        /**
         * Forza un controllo immediato
         */
        fun checkNow(context: Context, currentVersion: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(KEY_CURRENT_VERSION to currentVersion)

            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Controllo aggiornamenti immediato avviato")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentVersion = inputData.getString(KEY_CURRENT_VERSION) ?: return@withContext Result.failure()

            Log.d(TAG, "Inizio controllo aggiornamenti. Versione attuale: $currentVersion")

            val apiManager = GitHubApiManager()
            val latestRelease = apiManager.getLatestRelease()

            if (latestRelease == null) {
                Log.w(TAG, "Impossibile ottenere l'ultimo release")
                return@withContext Result.retry()
            }

            val latestVersion = latestRelease.tagName
            Log.d(TAG, "Ultima versione disponibile: $latestVersion")

            // Confronta le versioni
            val isNewer = apiManager.isVersionNewer(latestVersion, currentVersion)

            if (isNewer) {
                Log.d(TAG, "Nuova versione trovata: $latestVersion")

                // Trova l'asset APK
                val apkAsset = apiManager.findApkAsset(latestRelease)

                if (apkAsset != null) {
                    // Mostra notifica di aggiornamento disponibile
                    showUpdateNotification(latestRelease, apkAsset.downloadUrl)

                    // Restituisci i dati per il worker successivo
                    val outputData = workDataOf(
                        KEY_CHECK_RESULT to true,
                        KEY_NEW_VERSION to latestVersion,
                        KEY_DOWNLOAD_URL to apkAsset.downloadUrl
                    )

                    return@withContext Result.success(outputData)
                } else {
                    Log.w(TAG, "Nessun asset APK trovato nel release")
                }
            } else {
                Log.d(TAG, "L'app è già aggiornata")
            }

            val outputData = workDataOf(KEY_CHECK_RESULT to false)
            return@withContext Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il controllo aggiornamenti", e)
            return@withContext Result.retry()
        }
    }

    /**
     * Mostra una notifica per il nuovo aggiornamento disponibile
     */
    private fun showUpdateNotification(release: GitHubRelease, downloadUrl: String) {
        createNotificationChannel()

        // Intent per aprire l'app quando si clicca la notifica
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_update_dialog", true)
            putExtra("new_version", release.tagName)
            putExtra("download_url", downloadUrl)
            putExtra("release_notes", release.body)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.update_available))
            .setContentText(applicationContext.getString(R.string.update_available_desc, release.tagName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(applicationContext.getString(R.string.update_available_desc, release.tagName) + ". " +
                        applicationContext.getString(R.string.update_available_prompt, release.tagName) + "\n\n" + (release.body ?: applicationContext.getString(R.string.no_description))))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                applicationContext.getString(R.string.download),
                createDownloadPendingIntent(downloadUrl, release.tagName)
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Crea il PendingIntent per il download
     */
    private fun createDownloadPendingIntent(downloadUrl: String, version: String): PendingIntent {
        val intent = Intent(context, UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_START_DOWNLOAD
            putExtra(UpdateDownloadService.EXTRA_DOWNLOAD_URL, downloadUrl)
            putExtra(UpdateDownloadService.EXTRA_VERSION, version)
        }

        return PendingIntent.getService(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea il canale di notifica (solo per Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aggiornamenti App",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.update_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
