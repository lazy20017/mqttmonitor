package org.archuser.mqttnotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BrokerEntity::class,
        TopicSubscriptionEntity::class,
        MessageEntity::class,
        TopicCounterEntity::class,
        RetentionPolicyEntity::class,
        AppStateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerDao(): BrokerDao
    abstract fun topicSubscriptionDao(): TopicSubscriptionDao
    abstract fun messageDao(): MessageDao
    abstract fun topicCounterDao(): TopicCounterDao
    abstract fun retentionPolicyDao(): RetentionPolicyDao
    abstract fun appStateDao(): AppStateDao
}
