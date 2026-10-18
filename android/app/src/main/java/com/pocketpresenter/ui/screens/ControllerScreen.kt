package com.pocketpresenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketpresenter.input.TouchpadGestureProcessor
import com.pocketpresenter.session.SessionState
import com.pocketpresenter.ui.ControllerMode
import com.pocketpresenter.ui.components.AVAILABLE_EFFECTS
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
 * Controller Screen conforming to docs/app_flow.md Screen D and P7-03, P7-05.
 * Features mode selector (Touchpad/Slides), live effect strip, and session indicators.
 */
@Composable
fun ControllerScreen(
    sessionState: SessionState,
    phoneStatusLabel: String,
    connectedDeviceName: String?,
    controllerMode: ControllerMode,
    selectedEffectId: String,
    isDragLocked: Boolean,
    gestureProcessor: TouchpadGestureProcessor,
    onSetControllerMode: (ControllerMode) -> Unit,
    onSelectEffect: (String) -> Unit,
    onToggleDragLock: () -> Unit,
    onSendLeftClick: () -> Unit,
    onSendRightClick: () -> Unit,
    onNextSlide: () -> Unit,
    onPreviousSlide: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenPocketMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isControlsEnabled = sessionState is SessionState.PresentingArmed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBackground)
    ) {
        // Header (docs/app_flow.md Screen D)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (sessionState) {
                        is SessionState.PresentingArmed -> CyanAccent
                        is SessionState.ConnectedIdle -> CyanAccent.copy(alpha = 0.5f)
                        is SessionState.Connecting -> WarningAmber
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = phoneStatusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = connectedDeviceName ?: "Paired PC",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row {
                TextButton(onClick = onOpenPocketMode) {
                    Text("Pocket", color = CyanAccent)
                }
                TextButton(onClick = onNavigateToSettings) {
                    Text("Settings", color = TextSecondary)
                }
                TextButton(onClick = onNavigateToHome) {
                    Text("Exit", color = ErrorRed)
                }
            }
        }

        // Mode Selector: Touchpad | Slides
        TabRow(
            selectedTabIndex = if (controllerMode == ControllerMode.TOUCHPAD) 0 else 1,
            containerColor = GraphiteSurface,
            contentColor = CyanAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        tabPositions[if (controllerMode == ControllerMode.TOUCHPAD) 0 else 1]
                    ),
                    color = CyanAccent
                )
            }
        ) {
            Tab(
                selected = controllerMode == ControllerMode.TOUCHPAD,
                onClick = { onSetControllerMode(ControllerMode.TOUCHPAD) },
                text = {
                    Text(
                        text = "Touchpad",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (controllerMode == ControllerMode.TOUCHPAD) CyanAccent else TextSecondary
                    )
                }
            )
            Tab(
                selected = controllerMode == ControllerMode.SLIDES,
                onClick = { onSetControllerMode(ControllerMode.SLIDES) },
                text = {
                    Text(
                        text = "Slides",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (controllerMode == ControllerMode.SLIDES) CyanAccent else TextSecondary
                    )
                }
            )
        }

        // Interaction Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (controllerMode) {
                ControllerMode.TOUCHPAD -> {
                    TouchpadScreen(
                        gestureProcessor = gestureProcessor,
                        isControlsEnabled = isControlsEnabled,
                        isDragLocked = isDragLocked,
                        onToggleDragLock = onToggleDragLock,
                        onSendLeftClick = onSendLeftClick,
                        onSendRightClick = onSendRightClick
                    )
                }
                ControllerMode.SLIDES -> {
                    SlidesScreen(
                        isControlsEnabled = isControlsEnabled,
                        onNextSlide = onNextSlide,
                        onPreviousSlide = onPreviousSlide
                    )
                }
            }
        }

        // Live Effect Strip (docs/app_flow.md Screen D: Honeycomb, Diamond, Ripple, Off)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface)
                .border(1.dp, BorderSubtle)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVAILABLE_EFFECTS.forEach { effect ->
                    val isSelected = effect.id == selectedEffectId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GraphiteSurfaceVariant else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyanAccent else BorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectEffect(effect.id) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(effect.swatchColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = effect.name.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) CyanAccent else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
