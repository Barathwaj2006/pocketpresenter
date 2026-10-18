package com.pocketpresenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pocketpresenter.settings.UserSettings
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary

/**
 * Settings Screen conforming to docs/app_flow.md Screen I and P7-04.
 * Configures sensitivity, gestures, haptic feedback, and displays protocol diagnostics.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateReverseScroll: (Boolean) -> Unit,
    onUpdateHaptics: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done", color = CyanAccent)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Pointer & Touchpad
        Text(
            text = "POINTER & GESTURES",
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GraphiteSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Touchpad Sensitivity", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        String.format("%.1fx", settings.touchpadSensitivity),
                        color = CyanAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Slider(
                    value = settings.touchpadSensitivity,
                    onValueChange = onUpdateSensitivity,
                    valueRange = 0.5f..2.5f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Reverse Two-Finger Scroll", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("Inverts vertical wheel direction", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.reverseScroll,
                        onCheckedChange = onUpdateReverseScroll,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GraphiteBackground,
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = GraphiteSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Haptic Feedback", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("Vibrate on slide transitions and tap clicks", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.hapticsEnabled,
                        onCheckedChange = onUpdateHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GraphiteBackground,
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = GraphiteSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: About & Protocol Diagnostics
        Text(
            text = "SYSTEM & DIAGNOSTICS",
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GraphiteSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Protocol Version", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("1 (v1.1 schema)", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transport", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Bluetooth Classic RFCOMM (SPP)", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Keepalive Watchdog", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("3000 ms (ADR-0013)", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Privacy & Telemetry", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Zero cloud sync • No telemetry", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
