package org.archuser.mqttnotify.notifications

import android.app.Notification
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.InboundMessageRecord

interface NotificationController {
    fun createChannels()
    fun buildPersistentNotification(snapshot: ConnectionSnapshot): Notification
    fun updatePersistentNotification(snapshot: ConnectionSnapshot)
    fun notifyTopicMessage(brokerLabel: String, message: InboundMessageRecord)
}
