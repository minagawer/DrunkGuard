package com.minagawer.drunkguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastKnownForeground: String? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == null) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
            handler.removeCallbacks(monitorRunnable)
            handler.post(monitorRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_GUARD_ACTIVE, false)) {
            stopSelf()
            return
        }

        val foreground = getForegroundPackageViaEvents() ?: return

        // DrunkGuard itself is in foreground (e.g. BlockActivity showing) → reset so LINE
        // re-triggers when the user returns to LINE from the task switcher
        if (foreground == packageName) {
            lastKnownForeground = null
            return
        }

        if (foreground == LINE_PACKAGE && foreground != lastKnownForeground) {
            lastKnownForeground = foreground
            launchBlockActivity()
        } else if (foreground != LINE_PACKAGE) {
            lastKnownForeground = foreground
        }
    }

    /**
     * queryEvents は queryUsageStats より正確にフォアグラウンドアプリを検出できる。
     * MOVE_TO_FOREGROUND イベントの中で最後に発生したものが現在のフォアグラウンドアプリ。
     */
    private fun getForegroundPackageViaEvents(): String? {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10_000L, now)
        val event = UsageEvents.Event()
        var foreground: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foreground = event.packageName
            }
        }
        return foreground
    }

    private fun launchBlockActivity() {
        // SYSTEM_ALERT_WINDOW 権限がないと Android 10+ ではバックグラウンドからの
        // startActivity がブロックされる
        if (!Settings.canDrawOverlays(this)) return

        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "DrunkGuard",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "飲酒中のLINEアクセスを監視しています"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("DrunkGuard 稼働中")
        .setContentText("LINEへのアクセスを監視しています 🛡️")
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    companion object {
        const val PREFS_NAME = "DrunkGuardPrefs"
        const val KEY_GUARD_ACTIVE = "guardActive"
        const val LINE_PACKAGE = "jp.naver.line.android"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "drunkguard_channel"
        const val CHECK_INTERVAL_MS = 1_000L
    }
}
