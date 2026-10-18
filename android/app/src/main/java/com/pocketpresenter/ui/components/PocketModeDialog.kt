package com.pocketpresenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketpresenter.ui.DevicePocketModeCapability
import com.pocketpresenter.ui.PocketModeManager
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
 * Pocket Mode activation dialog as specified in docs/app_flow.md Screen H & P7-08.
 * Transparently indicates whether the host device's OS delivers screen-off volume events.
 */
@Composable
fun PocketModeDialog(
    onDismiss: () -> Unit,
    onConfirmPocketMode: () -> Unit,
    capability: DevicePocketModeCapability = PocketModeManager.evaluateDeviceCapability()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = {
            Text(
                text = "Pocket Mode",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Control your presentation from your pocket using hardware volume buttons without looking at your screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Capability Badge
                val (badgeBg, badgeText, badgeColor) = when (capability) {
                    DevicePocketModeCapability.VALIDATED ->
                        Triple(CyanAccent.copy(alpha = 0.15f), "Device Validated", CyanAccent)
                    DevicePocketModeCapability.NOT_VALIDATED ->
                        Triple(WarningAmber.copy(alpha = 0.15f), "Not Validated on this Phone", WarningAmber)
                    DevicePocketModeCapability.UNSUPPORTED ->
                        Triple(ErrorRed.copy(alpha = 0.15f), "Unsupported on this Device", ErrorRed)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "●",
                            color = badgeColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = PocketModeManager.getLimitationExplanation(capability),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GraphiteSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("• Volume Up: Next slide", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("• Volume Down: Previous slide", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("• Touchpad is disabled while screen is locked", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPocketMode()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enable Pocket Mode", color = GraphiteBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
