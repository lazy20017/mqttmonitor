package org.archuser.mqttnotify.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.archuser.mqttnotify.core.DefaultDispatchersProvider
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.SystemTimeProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.data.local.AppDatabase
import org.archuser.mqttnotify.data.local.AppStateDao
import org.archuser.mqttnotify.data.local.BrokerDao
import org.archuser.mqttnotify.data.local.MessageDao
import org.archuser.mqttnotify.data.local.RetentionPolicyDao
import org.archuser.mqttnotify.data.local.TopicCounterDao
import org.archuser.mqttnotify.data.local.TopicSubscriptionDao

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mqttnotify.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBrokerDao(db: AppDatabase): BrokerDao = db.brokerDao()
    @Provides fun provideTopicDao(db: AppDatabase): TopicSubscriptionDao = db.topicSubscriptionDao()
    @Provides fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
    @Provides fun provideTopicCounterDao(db: AppDatabase): TopicCounterDao = db.topicCounterDao()
    @Provides fun provideRetentionDao(db: AppDatabase): RetentionPolicyDao = db.retentionPolicyDao()
    @Provides fun provideAppStateDao(db: AppDatabase): AppStateDao = db.appStateDao()

    @Provides
    @Singleton
    fun provideDispatchersProvider(impl: DefaultDispatchersProvider): DispatchersProvider = impl

    @Provides
    @Singleton
    fun provideTimeProvider(impl: SystemTimeProvider): TimeProvider = impl
}
