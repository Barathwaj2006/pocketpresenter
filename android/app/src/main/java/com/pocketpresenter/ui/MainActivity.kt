package com.pocketpresenter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketpresenter.session.SessionState
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.PocketPresenterTheme
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PocketPresenterTheme {
                PocketPresenterApp()
            }
        }
    }
}

@Composable
fun PocketPresenterApp() {
    var sessionState by remember { mutableStateOf<SessionState>(SessionState.Disconnected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PocketPresenter",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (sessionState) {
                    is SessionState.Disconnected -> "Status: Disconnected"
                    is SessionState.PhoneReady -> "Status: Waiting for PowerPoint"
                    is SessionState.Connecting -> "Status: Connecting..."
                    is SessionState.ConnectedIdle -> "Status: Connected (Idle)"
                    is SessionState.PresentingArmed -> "Status: Presenting Armed"
                    is SessionState.Disconnecting -> "Status: Disconnecting..."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    sessionState = if (sessionState is SessionState.Disconnected) {
                        SessionState.PhoneReady
                    } else {
                        SessionState.Disconnected
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text(
                    text = if (sessionState is SessionState.Disconnected) "Start Session" else "Stop Session",
                    color = GraphiteBackground
                )
            }
        }
    }
}
