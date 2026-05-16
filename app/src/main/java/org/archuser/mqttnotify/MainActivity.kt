package org.archuser.mqttnotify

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mqtt_history", Context.MODE_PRIVATE)
    private val maxHistory = 5

    data class ServerConfig(
        val host: String,
        val port: String,
        val username: String,
        val subscribeTopic: String,
        val publishTopic: String
    )

    fun getHistory(): List<ServerConfig> {
        val json = prefs.getString("history", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ServerConfig(
                    host = obj.getString("host"),
                    port = obj.getString("port"),
                    username = obj.getString("username"),
                    subscribeTopic = obj.getString("subscribeTopic"),
                    publishTopic = obj.getString("publishTopic")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addConfig(config: ServerConfig) {
        val history = getHistory().toMutableList()
        history.removeAll { it.host == config.host && it.port == config.port }
        history.add(0, config)
        val trimmed = history.take(maxHistory)
        saveHistory(trimmed)
    }

    private fun saveHistory(history: List<ServerConfig>) {
        val array = JSONArray()
        history.forEach { config ->
            val obj = JSONObject().apply {
                put("host", config.host)
                put("port", config.port)
                put("username", config.username)
                put("subscribeTopic", config.subscribeTopic)
                put("publishTopic", config.publishTopic)
            }
            array.put(obj)
        }
        prefs.edit().putString("history", array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {

    private var mqttClient: Mqtt3AsyncClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val receivedMessages = mutableStateListOf<MessageItem>()
    private lateinit var historyManager: HistoryManager

    @SuppressLint("HardwareIds")
    private fun generateDeviceUsername(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        return "mqtt_${androidId.hashCode().toString(16)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyManager = HistoryManager(this)
        val defaultUsername = generateDeviceUsername()

        setContent {
            MaterialTheme {
                SimpleMQTTScreen(
                    messages = receivedMessages,
                    defaultUsername = defaultUsername,
                    historyManager = historyManager,
                    onConnect = { host, port, subscribeTopic, publishTopic, username, password, onResult ->
                        connect(host, port, subscribeTopic, publishTopic, username, password, onResult)
                        historyManager.addConfig(
                            HistoryManager.ServerConfig(host, port.toString(), username, subscribeTopic, publishTopic)
                        )
                    },
                    onDisconnect = { disconnect() },
                    onPublish = { message, publishTopic ->
                        publish(message, publishTopic)
                    },
                    onClearMessages = { receivedMessages.clear() }
                )
            }
        }
    }

    private fun connect(
        host: String,
        port: Int,
        subscribeTopic: String,
        publishTopic: String,
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        val activity = this
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "正在连接...", Toast.LENGTH_SHORT).show()
                }

                val builder = MqttClient.builder()
                    .useMqttVersion3()
                    .serverHost(host)
                    .serverPort(port)
                    .identifier("mqtt_client_${System.currentTimeMillis()}")

                if (username.isNotEmpty()) {
                    builder.simpleAuth()
                        .username(username)
                        .apply {
                            if (password.isNotEmpty()) {
                                this.password(password.toByteArray())
                            }
                        }
                        .applySimpleAuth()
                }

                mqttClient = builder.buildAsync()

                mqttClient?.connectWith()
                    ?.keepAlive(60)
                    ?.cleanSession(true)
                    ?.send()
                    ?.whenComplete { _, throwable ->
                        if (throwable != null) {
                            onResult(false)
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(activity, "连接失败: " + (throwable.message ?: "未知错误"), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            mqttClient?.subscribeWith()
                                ?.topicFilter(subscribeTopic)
                                ?.qos(MqttQos.AT_LEAST_ONCE)
                                ?.callback { publish: Mqtt3Publish ->
                                    val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    scope.launch(Dispatchers.Main) {
                                        val newMessage = MessageItem(
                                            topic = publish.topic.toString(),
                                            payload = payload,
                                            time = time,
                                            type = MessageType.RECEIVED
                                        )
                                        receivedMessages.add(0, newMessage)
                                    }
                                }
                                ?.send()
                                ?.whenComplete { subThrowable, _ ->
                                    if (subThrowable != null) {
                                        scope.launch(Dispatchers.Main) {
                                            Toast.makeText(activity, "订阅失败", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }

                            onResult(true)
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(activity, "连接成功", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "连接错误: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disconnect() {
        val activity = this
        scope.launch {
            try {
                mqttClient?.disconnect()?.join()
                mqttClient = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "已断开连接", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "断开错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun publish(message: String, publishTopic: String) {
        val activity = this
        if (mqttClient == null) {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(activity, "请先连接", Toast.LENGTH_SHORT).show()
            }
            return
        }

        scope.launch {
            try {
                mqttClient?.publishWith()
                    ?.topic(publishTopic)
                    ?.payload(message.toByteArray(StandardCharsets.UTF_8))
                    ?.qos(MqttQos.AT_LEAST_ONCE)
                    ?.send()
                    ?.whenComplete { _, throwable ->
                        if (throwable == null) {
                            scope.launch(Dispatchers.Main) {
                                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                val newMessage = MessageItem(
                                    topic = publishTopic,
                                    payload = message,
                                    time = time,
                                    type = MessageType.SENT
                                )
                                receivedMessages.add(0, newMessage)
                                Toast.makeText(activity, "发送成功", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(activity, "发送失败: " + (throwable.message ?: "未知错误"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "发送错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mqttClient?.disconnect()
    }
}

data class MessageItem(
    val topic: String,
    val payload: String,
    val time: String,
    val type: MessageType
)

enum class MessageType {
    SENT, RECEIVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMQTTScreen(
    messages: List<MessageItem>,
    defaultUsername: String,
    historyManager: HistoryManager,
    onConnect: (String, Int, String, String, String, String, (Boolean) -> Unit) -> Unit,
    onDisconnect: () -> Unit,
    onPublish: (String, String) -> Unit,
    onClearMessages: () -> Unit
) {
    var host by remember { mutableStateOf("broker.emqx.io") }
    var port by remember { mutableStateOf("1883") }
    var subscribeTopic by remember { mutableStateOf("test/subscribe") }
    var publishTopic by remember { mutableStateOf("test/publish") }
    var publishMessage by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(defaultUsername) }
    var password by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("未连接") }

    // Load history once
    val history by remember { mutableStateOf(historyManager.getHistory()) }

    LaunchedEffect(isConnected) {
        connectionStatus = if (isConnected) "已连接" else if (isConnecting) "连接中..." else "未连接"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MQTT 客户端") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ConnectionStatusCard(connectionStatus = connectionStatus)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ConnectionConfigCard(
                        host = host,
                        port = port,
                        subscribeTopic = subscribeTopic,
                        publishTopic = publishTopic,
                        username = username,
                        password = password,
                        connectionStatus = connectionStatus,
                        isConnecting = isConnecting,
                        history = history,
                        onHostChange = { host = it },
                        onPortChange = { port = it },
                        onSubscribeTopicChange = { subscribeTopic = it },
                        onPublishTopicChange = { publishTopic = it },
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onConfigSelect = { config ->
                            host = config.host
                            port = config.port
                            username = config.username
                            subscribeTopic = config.subscribeTopic
                            publishTopic = config.publishTopic
                        },
                        onConnect = {
                            isConnecting = true
                            connectionStatus = "连接中..."
                            onConnect(host, port.toIntOrNull() ?: 1883, subscribeTopic, publishTopic, username, password) { success ->
                                isConnecting = false
                                isConnected = success
                            }
                        },
                        onDisconnect = {
                            isConnecting = false
                            connectionStatus = "未连接"
                            onDisconnect()
                        }
                    )
                }

                item {
                    PublishMessageCard(
                        publishMessage = publishMessage,
                        connectionStatus = connectionStatus,
                        onPublishMessageChange = { publishMessage = it },
                        onPublish = {
                            if (publishMessage.isNotEmpty()) {
                                onPublish(publishMessage, publishTopic)
                                publishMessage = ""
                            }
                        },
                        onQuickCommand = { cmd ->
                            onPublish(cmd, publishTopic)
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("消息记录 (${messages.size})", style = MaterialTheme.typography.titleMedium)
                        if (messages.isNotEmpty()) {
                            TextButton(onClick = onClearMessages) {
                                Text("清空")
                            }
                        }
                    }
                }

                items(messages, key = { "${it.time}_${it.payload}" }) { message ->
                    MessageItemCard(message = message)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionStatus: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                connectionStatus == "已连接" -> MaterialTheme.colorScheme.primaryContainer
                connectionStatus == "连接中..." -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Text(
            text = "状态: $connectionStatus",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionConfigCard(
    host: String,
    port: String,
    subscribeTopic: String,
    publishTopic: String,
    username: String,
    password: String,
    connectionStatus: String,
    isConnecting: Boolean,
    history: List<HistoryManager.ServerConfig>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSubscribeTopicChange: (String) -> Unit,
    onPublishTopicChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfigSelect: (HistoryManager.ServerConfig) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    var hostExpanded by remember { mutableStateOf(false) }
    var usernameExpanded by remember { mutableStateOf(false) }
    var subscribeTopicExpanded by remember { mutableStateOf(false) }
    var publishTopicExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // History selector for host
            ExposedDropdownMenuBox(
                expanded = hostExpanded,
                onExpandedChange = { if (connectionStatus != "已连接") hostExpanded = it }
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text("服务器地址") },
                    placeholder = { Text("例如：broker.emqx.io") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    enabled = connectionStatus != "已连接",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostExpanded) }
                )
                if (history.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = hostExpanded,
                        onDismissRequest = { hostExpanded = false }
                    ) {
                        history.forEach { config ->
                            DropdownMenuItem(
                                text = { Text("${config.host}:${config.port}") },
                                onClick = {
                                    onConfigSelect(config)
                                    hostExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("端口号") },
                placeholder = { Text("默认：1883") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = connectionStatus != "已连接"
            )

            HorizontalDivider()

            Text("身份验证", style = MaterialTheme.typography.titleMedium)

            // History selector for username
            ExposedDropdownMenuBox(
                expanded = usernameExpanded,
                onExpandedChange = { if (connectionStatus != "已连接") usernameExpanded = it }
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("用户名") },
                    placeholder = { Text("根据设备自动生成") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    enabled = connectionStatus != "已连接",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = usernameExpanded) }
                )
                if (history.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = usernameExpanded,
                        onDismissRequest = { usernameExpanded = false }
                    ) {
                        history.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.username.ifEmpty { "(空)" }) },
                                onClick = {
                                    onConfigSelect(config)
                                    usernameExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("密码（可选）") },
                placeholder = { Text("留空表示不需要密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = connectionStatus != "已连接"
            )

            HorizontalDivider()

            Text("主题设置", style = MaterialTheme.typography.titleMedium)

            // History selector for subscribe topic
            ExposedDropdownMenuBox(
                expanded = subscribeTopicExpanded,
                onExpandedChange = { if (connectionStatus != "已连接") subscribeTopicExpanded = it }
            ) {
                OutlinedTextField(
                    value = subscribeTopic,
                    onValueChange = onSubscribeTopicChange,
                    label = { Text("订阅主题") },
                    placeholder = { Text("接收消息的主题") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    enabled = connectionStatus != "已连接",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subscribeTopicExpanded) }
                )
                if (history.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = subscribeTopicExpanded,
                        onDismissRequest = { subscribeTopicExpanded = false }
                    ) {
                        history.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.subscribeTopic) },
                                onClick = {
                                    onConfigSelect(config)
                                    subscribeTopicExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // History selector for publish topic
            ExposedDropdownMenuBox(
                expanded = publishTopicExpanded,
                onExpandedChange = { if (connectionStatus != "已连接") publishTopicExpanded = it }
            ) {
                OutlinedTextField(
                    value = publishTopic,
                    onValueChange = onPublishTopicChange,
                    label = { Text("发布主题") },
                    placeholder = { Text("发送消息的主题") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    enabled = connectionStatus != "已连接",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = publishTopicExpanded) }
                )
                if (history.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = publishTopicExpanded,
                        onDismissRequest = { publishTopicExpanded = false }
                    ) {
                        history.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.publishTopic) },
                                onClick = {
                                    onConfigSelect(config)
                                    publishTopicExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connectionStatus != "已连接") {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        enabled = !isConnecting
                    ) {
                        Text(if (isConnecting) "连接中..." else "连接服务器")
                    }
                } else {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("断开连接")
                    }
                }
            }
        }
    }
}

@Composable
fun PublishMessageCard(
    publishMessage: String,
    connectionStatus: String,
    onPublishMessageChange: (String) -> Unit,
    onPublish: () -> Unit,
    onQuickCommand: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("发送消息", style = MaterialTheme.typography.titleMedium)

            // Quick Command Buttons - LED
            Text("LED 控制", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"led":"led1","state":"on"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LED1 开")
                }
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"led":"led1","state":"off"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LED1 关")
                }
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"led":"led2","state":"on"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LED2 开")
                }
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"led":"led2","state":"off"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LED2 关")
                }
            }

            // Quick Command Buttons - Buzzer
            Text("蜂鸣器控制", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"buzzer":"on"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("蜂鸣器 开")
                }
                FilledTonalButton(
                    onClick = { onQuickCommand("""{"buzzer":"off"}""") },
                    enabled = connectionStatus == "已连接",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("蜂鸣器 关")
                }
                Spacer(modifier = Modifier.weight(2f))
            }

            OutlinedTextField(
                value = publishMessage,
                onValueChange = onPublishMessageChange,
                label = { Text("消息内容") },
                placeholder = { Text("输入要发送的消息...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (publishMessage.isNotEmpty()) {
                            onPublish()
                            focusManager.clearFocus()
                        }
                    }
                ),
                enabled = connectionStatus == "已连接"
            )

            Button(
                onClick = onPublish,
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionStatus == "已连接" && publishMessage.isNotEmpty()
            ) {
                Text("发送")
            }

            if (connectionStatus != "已连接") {
                Text(
                    "请先连接服务器才能发送消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun MessageItemCard(message: MessageItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.type == MessageType.SENT)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.type == MessageType.SENT) "发送" else "接收",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "主题: ${message.topic}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.payload,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MqttNotifyTheme {
        // 预览整个界面
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("MQTT Monitor Preview", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Broker: test.mqtt.com", style = MaterialTheme.typography.bodyLarge)
                Text("Status: Connected", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageItemPreview() {
    MqttNotifyTheme {
        MessageItem(
            id = "1",
            topic = "home/sensor/temperature",
            payload = "25.5°C",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }
}
