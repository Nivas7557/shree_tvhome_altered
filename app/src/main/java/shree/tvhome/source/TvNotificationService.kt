package shree.tvhome.source

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class TvNotificationService : NotificationListenerService() {

    data class AlertItem(
        val key: String,
        val appName: String,
        val title: String,
        val text: String,
        val timeStr: String
    )

    companion object {
        @Volatile
        private var instance: TvNotificationService? = null
        private val alerts = CopyOnWriteArrayList<AlertItem>()

        fun isPermissionGranted(context: Context): Boolean {
            return try {
                val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    ?: return false
                val cn = ComponentName(context, TvNotificationService::class.java)
                enabled.contains(cn.flattenToString()) || enabled.contains(cn.flattenToShortString())
            } catch (_: Exception) {
                false
            }
        }

        fun getAlerts(): List<AlertItem> {
            instance?.syncActiveNotifications()
            return alerts.toList()
        }

        fun dismissAlert(key: String) {
            alerts.removeAll { it.key == key }
            try {
                instance?.cancelNotification(key)
            } catch (_: Exception) {}
        }

        fun clearAllAlerts() {
            alerts.clear()
            try {
                instance?.cancelAllNotifications()
            } catch (_: Exception) {}
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        syncActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        syncActiveNotifications()
    }

    fun syncActiveNotifications() {
        val list = ArrayList<AlertItem>()
        try {
            val active = activeNotifications ?: emptyArray()
            val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
            for (sbn in active) {
                if (!sbn.isClearable) continue
                val pkg = sbn.packageName ?: "System"
                if (pkg == packageName) continue
                val appName = try {
                    val info = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    pkg.substringAfterLast(".")
                }
                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence("android.title")?.toString()?.trim().orEmpty()
                val body = extras?.getCharSequence("android.text")?.toString()?.trim()
                    ?: extras?.getCharSequence("android.bigText")?.toString()?.trim().orEmpty()
                if (title.isEmpty() && body.isEmpty()) continue
                val tStr = try { fmt.format(Date(sbn.postTime)) } catch (_: Exception) { "" }
                list.add(
                    AlertItem(
                        key = sbn.key ?: "${pkg}_${sbn.postTime}",
                        appName = appName,
                        title = if (title.isNotEmpty()) title else appName,
                        text = if (body.isNotEmpty()) body else "New alert",
                        timeStr = tStr
                    )
                )
            }
        } catch (_: Exception) {}
        alerts.clear()
        alerts.addAll(list)
    }
}
