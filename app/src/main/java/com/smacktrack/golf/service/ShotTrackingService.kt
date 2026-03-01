package com.smacktrack.golf.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smacktrack.golf.MainActivity
import com.smacktrack.golf.R

class ShotTrackingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel(
            CHANNEL_ID, "Shot Tracking", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmackTrack")
            .setContentText("Tracking your shot\u2026")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "shot_tracking"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, ShotTrackingService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShotTrackingService::class.java))
        }
    }
}
