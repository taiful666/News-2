package com.taiful.tfnewscraft.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.taiful.tfnewscraft.R

/**
 * Foreground service used while a Reel video is being exported:
 * keeps the app running when minimised or screen is off, holds a partial wake lock,
 * and shows export progress in the notification shade.
 */
class TFWorkService : Service() {

    companion object {
        const val ACTION_START = "tf.work.START"
        private const val CHANNEL_ID = "tf_export"
        private const val NOTIF_ID = 7310
        private const val DONE_ID = 7312

        @Volatile
        var instance: TFWorkService? = null
            private set
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTitle: String = ""
    private var lastNotifyMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.export_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.export_channel_desc)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent()).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun buildNotification(percent: Int, text: String, indeterminate: Boolean): Notification {
        val header = if (indeterminate) currentTitle else "$currentTitle · $percent%"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(header)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent, indeterminate)
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        ensureChannel()
        val extraTitle = intent?.getStringExtra("title")
        currentTitle = if (!extraTitle.isNullOrBlank()) extraTitle else getString(R.string.export_default_title)
        val initialText = intent?.getStringExtra("text").orEmpty()
        val notification = buildNotification(0, initialText, indeterminate = true)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (_: Throwable) {
            try {
                startForeground(NOTIF_ID, notification)
            } catch (_: Throwable) {
            }
        }

        if (wakeLock == null) {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tfnewscraft:export")?.apply {
                    setReferenceCounted(false)
                    acquire(60L * 60L * 1000L)
                }
            } catch (_: Throwable) {
            }
        }
        return START_NOT_STICKY
    }

    fun updateProgress(percent: Int, text: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotifyMs < 700L && percent < 100) return
        lastNotifyMs = now
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.notify(NOTIF_ID, buildNotification(percent.coerceIn(0, 100), text, indeterminate = false))
        } catch (_: Throwable) {
        }
    }

    fun finishWork(done: Boolean, doneTitle: String, doneText: String) {
        if (done) {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setContentTitle(doneTitle.ifBlank { getString(R.string.export_done_title) })
                    .setContentText(doneText)
                    .setAutoCancel(true)
                    .setContentIntent(openAppIntent())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                nm?.notify(DONE_ID, notification)
            } catch (_: Throwable) {
            }
        }
        stopSelf()
    }

    override fun onDestroy() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Throwable) {
        }
        wakeLock = null
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        if (instance === this) instance = null
        super.onDestroy()
    }
}
