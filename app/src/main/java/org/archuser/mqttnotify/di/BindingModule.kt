package org.archuser.mqttnotify.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.archuser.mqttnotify.connection.ConnectionCoordinator
import org.archuser.mqttnotify.connection.ConnectionCoordinatorImpl
import org.archuser.mqttnotify.data.mqtt.BrokerConnectionTester
import org.archuser.mqttnotify.data.mqtt.HiveBrokerConnectionTester
import org.archuser.mqttnotify.data.mqtt.HiveMqttClientAdapter
import org.archuser.mqttnotify.data.mqtt.MqttClientAdapter
import org.archuser.mqttnotify.data.repo.AppStateRepositoryImpl
import org.archuser.mqttnotify.data.repo.BrokerRepositoryImpl
import org.archuser.mqttnotify.data.repo.DiagnosticsRepositoryImpl
import org.archuser.mqttnotify.data.repo.MessageRepositoryImpl
import org.archuser.mqttnotify.data.repo.RetentionRepositoryImpl
import org.archuser.mqttnotify.data.repo.TopicRepositoryImpl
import org.archuser.mqttnotify.data.security.CredentialsStore
import org.archuser.mqttnotify.data.security.EncryptedCredentialsStore
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.RetentionRepository
import org.archuser.mqttnotify.domain.repo.TopicRepository
import org.archuser.mqttnotify.notifications.NotificationController
import org.archuser.mqttnotify.notifications.NotificationControllerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds @Singleton abstract fun bindMqttClientAdapter(impl: HiveMqttClientAdapter): MqttClientAdapter
    @Binds @Singleton abstract fun bindConnectionTester(impl: HiveBrokerConnectionTester): BrokerConnectionTester
    @Binds @Singleton abstract fun bindCredentialsStore(impl: EncryptedCredentialsStore): CredentialsStore
    @Binds @Singleton abstract fun bindNotificationController(impl: NotificationControllerImpl): NotificationController

    @Binds @Singleton abstract fun bindBrokerRepository(impl: BrokerRepositoryImpl): BrokerRepository
    @Binds @Singleton abstract fun bindTopicRepository(impl: TopicRepositoryImpl): TopicRepository
    @Binds @Singleton abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
    @Binds @Singleton abstract fun bindRetentionRepository(impl: RetentionRepositoryImpl): RetentionRepository
    @Binds @Singleton abstract fun bindAppStateRepository(impl: AppStateRepositoryImpl): AppStateRepository
    @Binds @Singleton abstract fun bindDiagnosticsRepository(impl: DiagnosticsRepositoryImpl): DiagnosticsRepository

    @Binds @Singleton abstract fun bindConnectionCoordinator(impl: ConnectionCoordinatorImpl): ConnectionCoordinator
}
