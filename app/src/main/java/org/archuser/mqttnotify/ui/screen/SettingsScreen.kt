package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onMuteForMinutes: (Int) -> Unit,
    onClearMute: () -> Unit,
    onMaterialYouChanged: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val formattedMuteUntil = state.muteUntil?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
    }
    val muteOptions = listOf(
        "15 minutes" to 15,
        "30 minutes" to 30,
        "1 hour" to 60,
        "2 hours" to 120,
        "8 hours" to 480
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf(muteOptions.first().first) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Global notification mute")
                Text(if (state.muted) "Muted until $formattedMuteUntil" else "Not muted")
                Button(onClick = { expanded = true }) { Text("Mute duration: $selectedLabel") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    muteOptions.forEach { (label, minutes) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedLabel = label
                                expanded = false
                                onMuteForMinutes(minutes)
                            }
                        )
                    }
                }
                Text(
                    "Tip: mute only suppresses notifications. Message ingestion and storage continue.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                Button(onClick = onClearMute) { Text("Clear mute") }
            }
        }

        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Material You")
                Switch(
                    checked = state.materialYouEnabled,
                    onCheckedChange = onMaterialYouChanged
                )
                Text(
                    "Tip: uses your wallpaper-derived dynamic colors on Android 12+.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Retention")
                Text("v1 uses default active retention policy with broker-level defaults.")
            }
        }

        Button(onClick = onOpenDiagnostics) {
            Text("Open Diagnostics")
        }
    }
}
