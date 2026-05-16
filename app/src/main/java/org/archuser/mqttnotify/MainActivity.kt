package org.archuser.mqttnotify

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.connection.ConnectionCoordinator
import org.archuser.mqttnotify.ui.navigation.AppNav
import org.archuser.mqttnotify.ui.theme.MqttNotifyTheme
import org.archuser.mqttnotify.ui.viewmodel.AppChromeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectionCoordinator: ConnectionCoordinator
    private val appChromeViewModel: AppChromeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val useMaterialYou by appChromeViewModel.materialYouEnabled.collectAsStateWithLifecycle()
            MqttNotifyTheme(useMaterialYou = useMaterialYou) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {}
                )
                var asked by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!asked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        asked = true
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNav(appChromeViewModel = appChromeViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            connectionCoordinator.onUiVisibilityChanged(true)
        }
    }

    override fun onStop() {
        lifecycleScope.launch {
            connectionCoordinator.onUiVisibilityChanged(false)
        }
        super.onStop()
    }
}
