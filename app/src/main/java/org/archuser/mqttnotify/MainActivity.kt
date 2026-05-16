package org.archuser.mqttnotify

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var mqttClient: Mqtt3AsyncClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val receivedMessages = mutableStateListOf<MessageItem>()

    @SuppressLint("HardwareIds")
    private fun generateDeviceUsername(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        return "mqtt_${androidId.hashCode().toString(16)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultUsername = generateDeviceUsername()

        setContent {
            MaterialTheme {
                SimpleMQTTScreen(
                    messages = receivedMessages,
                    defaultUsername = defaultUsername,
                    onConnect = { host, port, subscribeTopic, publishTopic, username, password, onResult ->
                        connect(host, port, subscribeTopic, publishTopic, username, password, onResult)
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
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "正在连接...", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@MainActivity, "连接失败: ${throwable.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            // 连接成功后再订阅
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
                                            Toast.makeText(this@MainActivity, "订阅失败: ${subThrowable.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }

                            onResult(true)
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "连接成功", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "连接错误: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disconnect() {
        scope.launch {
            try {
                mqttClient?.disconnect()?.join()
                mqttClient = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "已断开连接", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "断开错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun publish(message: String, publishTopic: String) {
        if (mqttClient == null) {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "请先连接", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@MainActivity, "发送成功", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "发送失败: ${throwable.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "发送错误: ${e.message}", Toast.LENGTH_SHORT).show()
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

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isConnected) {
        connectionStatus = if (isConnected) "已连接" else if (isConnecting) "连接中..." else "未连接"
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.scrollTo(0)
        }
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 连接状态
            Card(
                modifier = Modifier.fillMaxWidth(),
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

            // 连接配置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("服务器配置", style = MaterialTheme.typography.titleMedium)

                    // 服务器地址
                    Text("服务器地址", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = { Text("例如：broker.emqx.io") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = connectionStatus != "已连接"
                    )

                    // 端口
                    Text("端口号", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        placeholder = { Text("默认：1883") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = connectionStatus != "已连接"
                    )

                    HorizontalDivider()

                    Text("身份验证", style = MaterialTheme.typography.titleMedium)

                    // 用户名
                    Text("用户名", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("根据设备自动生成") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = connectionStatus != "已连接"
                    )

                    // 密码
                    Text("密码（可选）", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("留空表示不需要密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = connectionStatus != "已连接"
                    )

                    HorizontalDivider()

                    Text("主题设置", style = MaterialTheme.typography.titleMedium)

                    // 订阅主题
                    Text("订阅主题（接收消息）", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "设置要监听的主题，例如：sensor/data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = subscribeTopic,
                        onValueChange = { subscribeTopic = it },
                        placeholder = { Text("例如：sensor/data") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = connectionStatus != "已连接"
                    )

                    // 发布主题
                    Text("发布主题（发送消息）", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "发送消息时使用的主题前缀",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = publishTopic,
                        onValueChange = { publishTopic = it },
                        placeholder = { Text("例如：sensor/control") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = connectionStatus != "已连接"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (connectionStatus != "已连接") {
                            Button(
                                onClick = {
                                    isConnecting = true
                                    connectionStatus = "连接中..."
                                    onConnect(host, port.toIntOrNull() ?: 1883, subscribeTopic, publishTopic, username, password) { success ->
                                        isConnecting = false
                                        isConnected = success
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isConnecting
                            ) {
                                Text(if (isConnecting) "连接中..." else "连接服务器")
                            }
                        } else {
                            Button(
                                onClick = {
                                    isConnecting = false
                                    connectionStatus = "未连接"
                                    onDisconnect()
                                },
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

            // 发布消息
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("发送消息", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = publishMessage,
                        onValueChange = { publishMessage = it },
                        label = { Text("消息内容") },
                        placeholder = { Text("输入要发送的消息...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (publishMessage.isNotEmpty()) {
                                    onPublish(publishMessage, publishTopic)
                                    publishMessage = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        enabled = connectionStatus == "已连接"
                    )

                    Button(
                        onClick = {
                            if (publishMessage.isNotEmpty()) {
                                onPublish(publishMessage, publishTopic)
                                publishMessage = ""
                            }
                        },
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

            // 消息列表
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

            messages.forEach { message ->
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
                                style = MaterialTheme.typography.labelSmall
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
        }
    }
}
