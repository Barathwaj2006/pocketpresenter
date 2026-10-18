package com.pocketpresenter.ui.screens

import android.view.MotionEvent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.pocketpresenter.input.AndroidMouseButton
import com.pocketpresenter.input.TouchpadGestureProcessor
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary
import com.pocketpresenter.ui.theme.WarningAmber

/**
 * Touchpad mode surface conforming to docs/app_flow.md Screen E and P7-03.
 * Maps multi-touch gestures directly to TouchpadGestureProcessor without UI easing or lag.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    gestureProcessor: TouchpadGestureProcessor,
    isControlsEnabled: Boolean,
    isDragLocked: Boolean,
    onToggleDragLock: () -> Unit,
    onSendLeftClick: () -> Unit,
    onSendRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Main Touchpad Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(GraphiteSurface)
                .border(
                    width = 1.dp,
                    color = if (isControlsEnabled) CyanAccent.copy(alpha = 0.4f) else BorderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInteropFilter { motionEvent ->
                    if (!isControlsEnabled) {
                        return@pointerInteropFilter false
                    }

                    val count = motionEvent.pointerCount
                    val x1 = motionEvent.x
                    val y1 = motionEvent.y
                    val x2 = if (count > 1) motionEvent.getX(1) else 0f
                    val y2 = if (count > 1) motionEvent.getY(1) else 0f

                    val action = when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> TouchpadGestureProcessor.ACTION_DOWN
                        MotionEvent.ACTION_UP -> TouchpadGestureProcessor.ACTION_UP
                        MotionEvent.ACTION_MOVE -> TouchpadGestureProcessor.ACTION_MOVE
                        MotionEvent.ACTION_CANCEL -> TouchpadGestureProcessor.ACTION_CANCEL
                        MotionEvent.ACTION_POINTER_DOWN -> TouchpadGestureProcessor.ACTION_POINTER_DOWN
                        MotionEvent.ACTION_POINTER_UP -> TouchpadGestureProcessor.ACTION_POINTER_UP
                        else -> -1
                    }

                    if (action != -1) {
                        gestureProcessor.onTouchEvent(
                            action = action,
                            pointerCount = count,
                            x1 = x1,
                            y1 = y1,
                            x2 = x2,
                            y2 = y2,
                            eventTimeMs = motionEvent.eventTime
                        )
                    }
                    true
                },
            contentAlignment = Alignment.Center
        ) {
            if (!isControlsEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GraphiteBackground.copy(alpha = 0.85f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Slideshow Not Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start the slide show in PowerPoint to enable touchpad pointer movement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDragLocked) "DRAG LOCKED" else "Touchpad Active",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isDragLocked) CyanAccent else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1-finger: move • Tap: click • 2-finger: scroll",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessible Fallback Click & Drag Controls (docs/app_flow.md Screen E)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSendLeftClick,
                enabled = isControlsEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GraphiteSurfaceVariant,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Left Click", style = MaterialTheme.typography.titleSmall)
            }

            Button(
                onClick = onSendRightClick,
                enabled = isControlsEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GraphiteSurfaceVariant,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Right Click", style = MaterialTheme.typography.titleSmall)
            }

            OutlinedButton(
                onClick = onToggleDragLock,
                enabled = isControlsEnabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDragLocked) CyanAccent else TextSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isDragLocked) CyanAccent else BorderSubtle)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = if (isDragLocked) "Unlock" else "Drag Lock",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
