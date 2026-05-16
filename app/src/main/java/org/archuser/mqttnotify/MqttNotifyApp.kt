package org.archuser.mqttnotify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.notifications.NotificationController

@HiltAndroidApp
class MqttNotifyApp : Application() {

    @Inject lateinit var notificationController: NotificationController

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            notificationController.createChannels()
        }
    }
}
