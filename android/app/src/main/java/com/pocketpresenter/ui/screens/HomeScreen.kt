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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketpresenter.session.SessionState
import com.pocketpresenter.ui.components.AVAILABLE_EFFECTS
import com.pocketpresenter.ui.components.EffectSelector
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.ErrorRed
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary
import com.pocketpresenter.ui.theme.WarningAmber

/**
 * Home / Connection screen conforming to docs/app_flow.md Screen C and P7-02.
 * Shows state-driven controls, session start/stop, effect preview, and connection guidance.
 */
@Composable
fun HomeScreen(
    sessionState: SessionState,
    phoneStatusLabel: String,
    selectedEffectId: String,
    effectIntensity: Float,
    reducedMotion: Boolean,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onNavigateToController: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenPocketModeDialog: () -> Unit,
    onSelectEffect: (String) -> Unit,
    onUpdateIntensity: (Float) -> Unit,
    onUpdateReducedMotion: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isSessionActive = sessionState !is SessionState.Disconnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PocketPresenter",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "PowerPoint Bluetooth Controller",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Row {
                TextButton(onClick = onNavigateToSetup) {
                    Text("Setup", color = CyanAccent)
                }
                TextButton(onClick = onNavigateToSettings) {
                    Text("Settings", color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // State-Driven Status Banner (docs/app_flow.md §3)
        val (statusDotColor, statusBorderColor) = when (sessionState) {
            is SessionState.Disconnected -> Pair(TextSecondary, BorderSubtle)
            is SessionState.PhoneReady -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.5f))
            is SessionState.Connecting -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.5f))
            is SessionState.ConnectedIdle -> Pair(CyanAccent, CyanAccent.copy(alpha = 0.5f))
            is SessionState.PresentingArmed -> Pair(CyanAccent, CyanAccent.copy(alpha = 0.8f))
            is SessionState.Disconnecting -> Pair(ErrorRed, ErrorRed.copy(alpha = 0.5f))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, statusBorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = phoneStatusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Start / Stop Session Action Button
                Button(
                    onClick = {
                        if (isSessionActive) onStopSession() else onStartSession()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSessionActive) GraphiteSurfaceVariant else CyanAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isSessionActive) "End Session" else "Start Session",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSessionActive) ErrorRed else GraphiteBackground
                    )
                }

                if (sessionState is SessionState.ConnectedIdle || sessionState is SessionState.PresentingArmed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateToController,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Open Controller Surface", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connection Instructions Card (docs/app_flow.md Screen C)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface, RoundedCornerShape(14.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "PowerPoint Connection Guide",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Open PowerPoint on your Windows PC.\n" +
                            "2. Click the 'PocketPresenter' tab in the ribbon.\n" +
                            "3. Click 'Connect Phone' to initiate the RFCOMM link.\n" +
                            "4. Start the slide show (F5) to enable pointer & navigation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Effect Selector (Screen G) - Available in Connected Standby and Disconnected as local preference
        EffectSelector(
            selectedEffectId = selectedEffectId,
            intensity = effectIntensity,
            reducedMotion = reducedMotion,
            onEffectSelected = onSelectEffect,
            onIntensityChanged = onUpdateIntensity,
            onReducedMotionChanged = onUpdateReducedMotion
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pocket Mode Shortcut Button
        OutlinedButton(
            onClick = onOpenPocketModeDialog,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pocket Mode Configuration")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
