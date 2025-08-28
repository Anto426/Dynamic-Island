package com.anto426.dynamicisland.plugins.notification

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification

/**
 * Contiene i metadati estratti da una StatusBarNotification per un facile accesso nell'UI.
 *
 * @property title Il titolo della notifica.
 * @property body Il testo del corpo della notifica.
 * @property id L'ID univoco della notifica.
 * @property iconDrawable L'icona piccola della notifica come Drawable.
 * @property packageName Il nome del package dell'app che ha inviato la notifica.
 * @property actions La lista di azioni (es. rispondi, segna come letto) disponibili.
 * @property statusBarNotification L'oggetto StatusBarNotification originale per accedere a dati aggiuntivi.
 */
data class NotificationMeta(
	val title: String?,
	val body: String,
	val id: Int,
	val iconDrawable: Drawable,
	val packageName: String,
	val actions: List<Notification.Action>,
	val statusBarNotification: StatusBarNotification
) {
	/**
	 * Ottiene il nome visualizzabile dell'applicazione che ha inviato la notifica.
	 */
	fun getAppName(context: Context): String {
		return try {
			val pm = context.packageManager
			val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
			pm.getApplicationLabel(appInfo).toString()
		} catch (e: PackageManager.NameNotFoundException) {
			packageName // Fallback al nome del package se non si trova l'app
		}
	}
}