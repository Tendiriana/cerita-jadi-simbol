package id.taktik.xmod

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Hanya MENGHITUNG jumlah notifikasi aktif untuk lencana di status bar X-Mod.
 * Tidak membaca/menyimpan isi notifikasi.
 */
class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() { update() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { update() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { update() }

    private fun update() {
        count = try {
            activeNotifications?.count { sbn ->
                !sbn.isOngoing && (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            } ?: 0
        } catch (e: Exception) { 0 }
    }

    companion object {
        @JvmStatic var count = 0
    }
}
