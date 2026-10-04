package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.InventoryItem

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "rasoi_expiry_alarms"
        const val CHANNEL_NAME = "RasoiGuard Expiry Alarms"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical warnings for food items about to expire"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendExpiryAlert(item: InventoryItem) {
        val hours = item.hoursLeft()
        val title = if (hours <= 24) "🚨 Critical Expiry Alert: ${item.name}" else "⚠️ Use Soon: ${item.name}"
        val message = "Your ${item.quantity} ${item.unit} of ${item.name} expires in $hours hours. Tap to see zero-waste recipes!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(item.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // In case POST_NOTIFICATIONS runtime permission is not yet granted
        }
    }

    fun sendTestAlarm(customMessage: String = "RasoiGuard live alert engine verified active.") {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 RasoiGuard Notification Engine Active")
            .setContentText(customMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(9999, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
