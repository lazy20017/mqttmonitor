package org.archuser.mqttnotify.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.archuser.mqttnotify.MainActivity
import org.archuser.mqttnotify.R
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.service.PersistentConnectionService

@Singleton
class NotificationControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationController {

    private val manager = NotificationManagerCompat.from(context)
    private val alertIds = AtomicInteger(2000)

    override fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val serviceChannel = NotificationChannel(
            NotificationIds.CHANNEL_PERSISTENT,
            "MQTT Persistent Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active MQTT foreground connection state"
            setShowBadge(false)
        }

        val messageChannel = NotificationChannel(
            NotificationIds.CHANNEL_MESSAGES,
            "MQTT Topic Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Per-topic message notifications with banner pop-on-screen"
            enableVibration(true)
            enableLights(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val sysManager = context.getSystemService(NotificationManager::class.java)
        sysManager.createNotificationChannel(serviceChannel)
        sysManager.createNotificationChannel(messageChannel)
    }

    override fun buildPersistentNotification(snapshot: ConnectionSnapshot): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, PersistentConnectionService::class.java).setAction(PersistentConnectionService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = snapshot.brokerLabel?.let { "MQTT: $it" } ?: "MQTT monitor"
        val statusText = when (snapshot.status) {
            ConnectionStatus.CONNECTED -> "Connected"
            ConnectionStatus.CONNECTING -> "Connecting"
            ConnectionStatus.ERROR -> "Error"
            ConnectionStatus.DISCONNECTED -> "Disconnected"
        }

        val elapsed = snapshot.connectedSince?.let {
            val sec = ((System.currentTimeMillis() - it) / 1000L).coerceAtLeast(0L)
            val h = sec / 3600
            val m = (sec % 3600) / 60
            val s = sec % 60
            "%02d:%02d:%02d".format(h, m, s)
        } ?: "00:00:00"

        val content = "Status: $statusText | Elapsed: $elapsed | Messages: ${snapshot.messageCount}"

        return NotificationCompat.Builder(context, NotificationIds.CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_stat_mqtt)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Open", openIntent)
            .addAction(0, "Stop persistent mode", stopIntent)
            .build()
    }

    override fun updatePersistentNotification(snapshot: ConnectionSnapshot) {
        manager.notify(NotificationIds.PERSISTENT_ID, buildPersistentNotification(snapshot))
    }

    override fun notifyTopicMessage(brokerLabel: String, message: InboundMessageRecord) {
        val openIntent = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val content = if (message.payloadPreview.isBlank()) "<empty payload>" else message.payloadPreview
        val title = "[$brokerLabel] ${message.topic}"

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_mqtt)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(alertIds.incrementAndGet(), notification)
    }
}
