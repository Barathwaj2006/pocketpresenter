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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary
import com.pocketpresenter.ui.theme.WarningAmber

/**
 * Slides navigation screen conforming to docs/app_flow.md Screen F and P7-03.
 * Features large thumb-friendly Previous / Next touch targets and volume button indicators.
 */
@Composable
fun SlidesScreen(
    isControlsEnabled: Boolean,
    onNextSlide: () -> Unit,
    onPreviousSlide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Volume Key Shortcut Reminder Banner (docs/app_flow.md Screen F)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GraphiteSurfaceVariant)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hardware Keys: Vol Up = Next • Vol Down = Previous",
                style = MaterialTheme.typography.labelMedium,
                color = CyanAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isControlsEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
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
                        text = "Start the presentation in PowerPoint to enable slide navigation buttons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Giant Previous & Next Navigation Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Next Slide Target (Top / Primary)
                Button(
                    onClick = onNextSlide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = GraphiteBackground
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.4f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NEXT",
                            style = MaterialTheme.typography.headlineLarge,
                            color = GraphiteBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Advance slide or animation",
                            style = MaterialTheme.typography.labelSmall,
                            color = GraphiteBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                // Previous Slide Target (Bottom / Secondary)
                Button(
                    onClick = onPreviousSlide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphiteSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PREVIOUS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Step backward",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
