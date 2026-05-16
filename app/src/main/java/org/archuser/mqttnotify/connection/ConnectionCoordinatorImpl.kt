package org.archuser.mqttnotify.connection

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.core.TopicMatcher
import org.archuser.mqttnotify.data.mqtt.MqttClientAdapter
import org.archuser.mqttnotify.data.mqtt.MqttEvent
import org.archuser.mqttnotify.data.security.CredentialsStore
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.TopicRepository
import org.archuser.mqttnotify.notifications.NotificationController

@Singleton
class ConnectionCoordinatorImpl @Inject constructor(
    private val mqttClientAdapter: MqttClientAdapter,
    private val brokerRepository: BrokerRepository,
    private val topicRepository: TopicRepository,
    private val messageRepository: MessageRepository,
    private val appStateRepository: AppStateRepository,
    private val credentialsStore: CredentialsStore,
    private val notifications: NotificationController,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val dispatchers: DispatchersProvider,
    private val timeProvider: TimeProvider
) : ConnectionCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val reconcileSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private val lock = Mutex()

    private val _snapshot = MutableStateFlow(
        ConnectionSnapshot(
            status = ConnectionStatus.DISCONNECTED,
            brokerLabel = null,
            connectedSince = null,
            messageCount = 0,
            lastError = null
        )
    )

    override val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    private var uiVisible: Boolean = false
    private var persistentRunning: Boolean = false
    private var connectedBrokerId: Long? = null
    private var activeBroker: BrokerConfig? = null
    private var activeTopics: List<TopicSubscriptionConfig> = emptyList()
    private val subscribedTopics = mutableSetOf<String>()
    private var topicWatchJob: Job? = null

    init {
        scope.launch {
            appStateRepository.observeState().collectLatest {
                reconcileSignal.tryEmit(Unit)
            }
        }

        scope.launch {
            mqttClientAdapter.events().collectLatest { event ->
                when (event) {
                    is MqttEvent.ConnectionChanged -> handleConnectionState(event.status)
                    is MqttEvent.MessageReceived -> handleMessage(event)
                    is MqttEvent.SubscriptionAck -> diagnosticsRepository.log("Subscribed: ${event.topic}")
                    is MqttEvent.Error -> {
                        diagnosticsRepository.log("MQTT error: ${event.message}")
                        _snapshot.value = _snapshot.value.copy(lastError = event.message, status = ConnectionStatus.ERROR)
                    }
                }
            }
        }

        scope.launch {
            reconcileSignal.collectLatest {
                reconcile()
            }
        }

        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun setMode(mode: ConnectionMode) {
        appStateRepository.setConnectionMode(mode)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun setActiveBroker(brokerId: Long?) {
        appStateRepository.setActiveBroker(brokerId)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun onUiVisibilityChanged(visible: Boolean) {
        uiVisible = visible
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun startPersistent() {
        persistentRunning = true
        appStateRepository.setConnectionMode(ConnectionMode.PERSISTENT_FOREGROUND)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun stopPersistent() {
        persistentRunning = false
        appStateRepository.setConnectionMode(ConnectionMode.VISIBLE_ONLY)
        reconcileSignal.tryEmit(Unit)
    }

    private suspend fun reconcile() = lock.withLock {
        val state = appStateRepository.currentState()
        val shouldConnect = when (state.connectionMode) {
            ConnectionMode.VISIBLE_ONLY -> uiVisible
            ConnectionMode.PERSISTENT_FOREGROUND -> persistentRunning
        }

        val targetBroker = state.activeBrokerId
        if (!shouldConnect || targetBroker == null) {
            disconnectInternal("No active connection target")
            return
        }

        if (connectedBrokerId == targetBroker && snapshot.value.status == ConnectionStatus.CONNECTED) {
            return
        }

        connectToBroker(targetBroker)
    }

    private suspend fun connectToBroker(brokerId: Long) {
        disconnectInternal("Switching broker")
        val broker = brokerRepository.getBroker(brokerId)
        if (broker == null) {
            _snapshot.value = _snapshot.value.copy(
                status = ConnectionStatus.ERROR,
                lastError = "Broker not found"
            )
            diagnosticsRepository.log("Connection failed: broker $brokerId not found")
            return
        }

        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.CONNECTING,
            brokerLabel = broker.label,
            connectedSince = null,
            messageCount = 0,
            lastError = null
        )

        val password = broker.credentialsRef?.alias?.let { credentialsStore.getPassword(it) }
        val result = mqttClientAdapter.connect(broker, password)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Connection failed"
            _snapshot.value = _snapshot.value.copy(status = ConnectionStatus.ERROR, lastError = message)
            diagnosticsRepository.log("Connection failed to ${broker.label}: $message")
            return
        }

        connectedBrokerId = broker.id
        activeBroker = broker
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.CONNECTED,
            connectedSince = timeProvider.nowMillis(),
            messageCount = 0,
            lastError = null,
            brokerLabel = broker.label
        )
        appStateRepository.setLastSessionStartedAt(timeProvider.nowMillis())
        diagnosticsRepository.log("Connected to ${broker.label}")

        topicWatchJob?.cancel()
        topicWatchJob = scope.launch {
            topicRepository.observeTopicsForBroker(broker.id).collectLatest { topics ->
                syncSubscriptions(topics)
            }
        }
    }

    private suspend fun syncSubscriptions(topics: List<TopicSubscriptionConfig>) {
        activeTopics = topics
        if (connectedBrokerId == null || snapshot.value.status != ConnectionStatus.CONNECTED) {
            return
        }

        val wanted = topics.filter { it.enabled }.map { it.topicFilter }.toSet()
        val toAdd = wanted - subscribedTopics
        val toRemove = subscribedTopics - wanted

        toRemove.forEach { topic -> mqttClientAdapter.unsubscribe(topic) }
        toAdd.forEach { topic ->
            val qos = topics.firstOrNull { it.topicFilter == topic }?.qos ?: 1
            mqttClientAdapter.subscribe(topic, qos)
        }

        subscribedTopics.clear()
        subscribedTopics.addAll(wanted)
    }

    private suspend fun handleMessage(event: MqttEvent.MessageReceived) {
        val brokerId = connectedBrokerId ?: return
        val brokerLabel = activeBroker?.label ?: "MQTT"

        val record = messageRepository.ingestMessage(brokerId, event)
        _snapshot.value = _snapshot.value.copy(messageCount = _snapshot.value.messageCount + 1)

        val matchedTopic = activeTopics
            .filter { it.notifyEnabled && TopicMatcher.matches(it.topicFilter, event.topic) }
            .maxByOrNull { it.topicFilter.length }

        val appState = appStateRepository.currentState()
        val muted = appState.globalMuteUntil?.let { it > timeProvider.nowMillis() } ?: false

        if (matchedTopic != null && !muted && record.isNewActivity) {
            notifications.notifyTopicMessage(brokerLabel, record)
        }
    }

    private suspend fun handleConnectionState(status: ConnectionStatus) {
        _snapshot.value = when (status) {
            ConnectionStatus.CONNECTED -> _snapshot.value.copy(status = status, lastError = null)
            ConnectionStatus.CONNECTING -> _snapshot.value.copy(status = status)
            ConnectionStatus.DISCONNECTED -> _snapshot.value.copy(
                status = status,
                connectedSince = null,
                messageCount = 0
            )
            ConnectionStatus.ERROR -> _snapshot.value.copy(status = status)
        }
    }

    private suspend fun disconnectInternal(reason: String) {
        topicWatchJob?.cancel()
        topicWatchJob = null
        if (connectedBrokerId != null || snapshot.value.status != ConnectionStatus.DISCONNECTED) {
            mqttClientAdapter.disconnect()
            diagnosticsRepository.log("Disconnected: $reason")
        }
        connectedBrokerId = null
        activeBroker = null
        activeTopics = emptyList()
        subscribedTopics.clear()
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            connectedSince = null,
            messageCount = 0
        )
    }
}
