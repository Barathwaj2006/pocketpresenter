package com.pocketpresenter.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary

/**
 * Setup and Permission Screen conforming to docs/app_flow.md Screens A & B and P7-01.
 * Explains Bluetooth requirements clearly without overwhelming the presenter.
 */
@Composable
fun SetupScreen(
    hasBluetoothPermission: Boolean,
    isBluetoothEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Headline
        Text(
            text = "Welcome to PocketPresenter",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "PocketPresenter controls PowerPoint slide navigation and pointer effects from your phone over local Bluetooth Classic.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Three Requirement Cards
        SetupRequirementCard(
            title = "1. PowerPoint Add-in",
            description = "Ensure the PocketPresenter add-in is active in PowerPoint on your Windows PC.",
            isCompleted = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupRequirementCard(
            title = "2. Bluetooth Classic Pairing",
            description = if (isBluetoothEnabled) "Bluetooth is on. Pair your phone and PC in OS Bluetooth settings." else "Bluetooth is disabled. Turn on Bluetooth in system settings.",
            isCompleted = isBluetoothEnabled,
            actionLabel = "Open Bluetooth Settings",
            onAction = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupRequirementCard(
            title = "3. Nearby Device Permission",
            description = if (hasBluetoothPermission) "Nearby device permission granted." else "Android requires nearby device permission to discover and connect to paired PCs.",
            isCompleted = hasBluetoothPermission,
            actionLabel = if (!hasBluetoothPermission) "Grant Permission" else null,
            onAction = onRequestPermission
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Continue to Controller",
                style = MaterialTheme.typography.titleMedium,
                color = GraphiteBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        ) {
            Text("App Permissions in Settings", color = TextSecondary)
        }
    }
}

@Composable
fun SetupRequirementCard(
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GraphiteSurface)
            .border(1.dp, if (isCompleted) CyanAccent.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) "✓" else "○",
                    color = if (isCompleted) CyanAccent else TextSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyanAccent)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
