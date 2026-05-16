package org.archuser.mqttnotify.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val BROKERS = "brokers"
    const val BROKER_EDIT = "broker_edit/{brokerId}"
    const val TOPICS = "topics/{brokerId}"
    const val MESSAGES = "messages/{brokerId}"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"

    fun brokerEdit(brokerId: Long) = "broker_edit/$brokerId"
    fun topics(brokerId: Long) = "topics/$brokerId"
    fun messages(brokerId: Long) = "messages/$brokerId"
}
