package org.archuser.mqttnotify.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.service.PersistentConnectionService
import org.archuser.mqttnotify.ui.screen.BrokerEditScreen
import org.archuser.mqttnotify.ui.screen.BrokerListScreen
import org.archuser.mqttnotify.ui.screen.DashboardScreen
import org.archuser.mqttnotify.ui.screen.DiagnosticsScreen
import org.archuser.mqttnotify.ui.screen.MessageFeedScreen
import org.archuser.mqttnotify.ui.screen.SettingsScreen
import org.archuser.mqttnotify.ui.screen.TopicScreen
import org.archuser.mqttnotify.ui.viewmodel.AppChromeViewModel
import org.archuser.mqttnotify.ui.viewmodel.BrokerEditViewModel
import org.archuser.mqttnotify.ui.viewmodel.BrokerListViewModel
import org.archuser.mqttnotify.ui.viewmodel.DashboardViewModel
import org.archuser.mqttnotify.ui.viewmodel.DiagnosticsViewModel
import org.archuser.mqttnotify.ui.viewmodel.MessageFeedViewModel
import org.archuser.mqttnotify.ui.viewmodel.SettingsViewModel
import org.archuser.mqttnotify.ui.viewmodel.TopicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(appChromeViewModel: AppChromeViewModel) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: Routes.DASHBOARD
    val currentSection = routeSection(currentRoute)
    val muted by appChromeViewModel.muted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun navigateToRoot(route: String) {
        if (!navController.popBackStack(route, false)) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routeTitle(currentRoute)) },
                actions = {
                    IconButton(
                        onClick = {
                            appChromeViewModel.toggleMute()
                            toast(if (muted) "Notifications unmuted" else "Notifications muted for 15 minutes")
                        }
                    ) {
                        Icon(
                            imageVector = if (muted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Toggle mute"
                        )
                    }
                }
            )
        },
        bottomBar = {
            val items = listOf(
                Triple(Routes.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                Triple(Routes.BROKERS, Icons.Default.Router, "Brokers"),
                Triple(Routes.SETTINGS, Icons.Default.Build, "Settings")
            )
            NavigationBar {
                items.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        selected = currentSection == route,
                        onClick = { navigateToRoot(route) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = androidx.compose.ui.Modifier.padding(paddingValues)
        ) {
            composable(Routes.DASHBOARD) {
                val vm: DashboardViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                DashboardScreen(
                    state = state,
                    onSelectBroker = { brokerId ->
                        val wasActive = brokerId != null && state.activeBrokerId == brokerId
                        vm.selectBroker(brokerId)
                        if (wasActive) {
                            toast("Active broker cleared")
                        } else if (brokerId != null) {
                            val label = state.brokers.firstOrNull { broker -> broker.id == brokerId }?.label ?: "broker"
                            toast("Active broker set: $label")
                        }
                    },
                    onSetMode = {
                        vm.setMode(it)
                        toast(if (it == ConnectionMode.VISIBLE_ONLY) "Mode: Active while visible" else "Mode: Persistent foreground")
                    },
                    onOpenTopics = { navController.navigate(Routes.topics(it)) },
                    onOpenMessages = { navController.navigate(Routes.messages(it)) },
                    onStartPersistent = {
                        vm.setMode(ConnectionMode.PERSISTENT_FOREGROUND)
                        PersistentConnectionService.start(context)
                        toast("Persistent connection started")
                    },
                    onStopPersistent = {
                        vm.setMode(ConnectionMode.VISIBLE_ONLY)
                        PersistentConnectionService.stop(context)
                        toast("Persistent connection stopped")
                    }
                )
            }

            composable(Routes.BROKERS) {
                val vm: BrokerListViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                BrokerListScreen(
                    state = state,
                    onAdd = { navController.navigate(Routes.brokerEdit(0L)) },
                    onEdit = { navController.navigate(Routes.brokerEdit(it)) },
                    onDelete = {
                        vm.deleteBroker(it)
                        toast("Broker deleted")
                    },
                    onSetActive = { brokerId ->
                        val wasActive = state.activeBrokerId == brokerId
                        vm.setActiveBroker(brokerId)
                        if (wasActive) {
                            toast("Active broker cleared")
                        } else {
                            val label = state.brokers.firstOrNull { broker -> broker.id == brokerId }?.label ?: "broker"
                            toast("Using $label")
                        }
                    },
                    onOpenTopics = { navController.navigate(Routes.topics(it)) },
                    onOpenMessages = { navController.navigate(Routes.messages(it)) }
                )
            }

            composable(
                route = Routes.BROKER_EDIT,
                arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
            ) {
                val vm: BrokerEditViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(state.status) {
                    if (!state.status.isNullOrBlank()) {
                        toast(state.status ?: "")
                    }
                }

                BrokerEditScreen(
                    state = state,
                    onChange = { newState -> vm.update { newState } },
                    onTest = vm::testConnection,
                    onSave = {
                        vm.save {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                route = Routes.TOPICS,
                arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
            ) {
                val vm: TopicViewModel = hiltViewModel()
                val topics by vm.topics.collectAsStateWithLifecycle()
                TopicScreen(
                    topics = topics,
                    onAddTopic = { filter, qos, notifyEnabled, retainedAsNew ->
                        vm.addTopic(filter, qos, notifyEnabled, retainedAsNew)
                        toast("Topic saved")
                    },
                    onToggleEnabled = { item, enabled ->
                        vm.toggleEnabled(item, enabled)
                        toast(if (enabled) "Topic enabled" else "Topic disabled")
                    },
                    onDelete = {
                        vm.delete(it)
                        toast("Topic deleted")
                    }
                )
            }

            composable(
                route = Routes.MESSAGES,
                arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
            ) {
                val vm: MessageFeedViewModel = hiltViewModel()
                val messages by vm.messages.collectAsStateWithLifecycle()
                MessageFeedScreen(
                    messages = messages,
                    onReadAll = {
                        vm.markAllRead()
                        toast("All messages marked as read")
                    },
                    onMarkMessageRead = {
                        vm.markMessageRead(it)
                        toast("Message marked as read")
                    },
                    onMarkMessageUnread = {
                        vm.markMessageUnread(it)
                        toast("Message marked as unread")
                    },
                    onDeleteMessage = {
                        vm.deleteMessage(it)
                        toast("Message deleted")
                    }
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onMuteForMinutes = {
                        vm.muteFor(it)
                        toast("Notifications muted for $it minutes")
                    },
                    onClearMute = {
                        vm.clearMute()
                        toast("Notification mute cleared")
                    },
                    onMaterialYouChanged = {
                        vm.setMaterialYouEnabled(it)
                        toast(if (it) "Material You enabled" else "Material You disabled")
                    },
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
                )
            }

            composable(Routes.DIAGNOSTICS) {
                val vm: DiagnosticsViewModel = hiltViewModel()
                val snapshot by vm.snapshot.collectAsStateWithLifecycle()
                val events by vm.events.collectAsStateWithLifecycle()
                DiagnosticsScreen(snapshot = snapshot, events = events)
            }
        }
    }
}

private fun routeSection(route: String): String = when {
    route.startsWith("dashboard") -> Routes.DASHBOARD
    route.startsWith("brokers") -> Routes.BROKERS
    route.startsWith("broker_edit") -> Routes.BROKERS
    route.startsWith("topics") -> Routes.BROKERS
    route.startsWith("messages") -> Routes.BROKERS
    route.startsWith("settings") -> Routes.SETTINGS
    route.startsWith("diagnostics") -> Routes.SETTINGS
    else -> Routes.DASHBOARD
}

private fun routeTitle(route: String): String = when {
    route.startsWith("dashboard") -> "Dashboard"
    route.startsWith("brokers") -> "Brokers"
    route.startsWith("broker_edit") -> "Broker Editor"
    route.startsWith("topics") -> "Topics"
    route.startsWith("messages") -> "Message Feed"
    route.startsWith("settings") -> "Settings"
    route.startsWith("diagnostics") -> "Diagnostics"
    else -> "MQTT Monitor"
}
