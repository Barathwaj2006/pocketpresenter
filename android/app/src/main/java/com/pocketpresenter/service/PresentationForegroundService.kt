package com.pocketpresenter.service

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
import androidx.core.app.NotificationCompat
import com.pocketpresenter.ui.MainActivity

/**
 * Foreground service ensuring continuous Bluetooth connection and slide control
 * even when the screen is dimmed or Pocket Mode is active.
 * Uses FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE on Android 14+ (API 34).
 */
class PresentationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "pocketpresenter_session"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.pocketpresenter.action.START_FOREGROUND"
        const val ACTION_STOP = "com.pocketpresenter.action.STOP_FOREGROUND"
        const val ACTION_UPDATE_STATUS = "com.pocketpresenter.action.UPDATE_STATUS"
        const val EXTRA_STATUS = "extra_status"

        fun start(context: Context, status: String = "Waiting for PowerPoint") {
            val intent = Intent(context, PresentationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATUS, status)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateStatus(context: Context, status: String) {
            val intent = Intent(context, PresentationForegroundService::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_STATUS, status)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PresentationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentStatus: String = "Waiting for PowerPoint"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_UPDATE_STATUS -> {
                intent.getStringExtra(EXTRA_STATUS)?.let {
                    currentStatus = it
                    val notification = buildNotification(currentStatus)
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }

            ACTION_START, null -> {
                intent?.getStringExtra(EXTRA_STATUS)?.let { currentStatus = it }
                val notification = buildNotification(currentStatus)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PocketPresenter Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps presentation controller connected"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PresentationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketPresenter")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Session", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
