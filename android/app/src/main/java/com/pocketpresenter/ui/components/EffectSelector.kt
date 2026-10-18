package com.pocketpresenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketpresenter.ui.theme.BorderSubtle
import com.pocketpresenter.ui.theme.CyanAccent
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.GraphiteSurface
import com.pocketpresenter.ui.theme.GraphiteSurfaceVariant
import com.pocketpresenter.ui.theme.TextPrimary
import com.pocketpresenter.ui.theme.TextSecondary

data class EffectItem(
    val id: String,
    val name: String,
    val description: String,
    val swatchColor: Color
)

val AVAILABLE_EFFECTS = listOf(
    EffectItem("honeycomb", "Honeycomb Pulse", "Hexagonal cells with dual-tone depth", CyanAccent),
    EffectItem("diamond", "Diamond Facets", "Directional diamond lattice highlight", Color(0xFF38BDF8)),
    EffectItem("ripple", "Geometric Ripple", "Subtle concentric geometric contours", Color(0xFF818CF8)),
    EffectItem("off", "Off", "Standard cursor without visual overlay", Color(0xFF64748B))
)

/**
 * Effect selector conforming to docs/app_flow.md Screen G & docs/implementation_plan.md P7-04.
 * Live effect switching never interrupts presentation or shifts cursor position.
 */
@Composable
fun EffectSelector(
    selectedEffectId: String,
    intensity: Float,
    reducedMotion: Boolean,
    onEffectSelected: (String) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onReducedMotionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GraphiteSurface, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Cursor Effect",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 4 Effect Options in a Grid/Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AVAILABLE_EFFECTS.forEach { effect ->
                val isSelected = effect.id == selectedEffectId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GraphiteSurfaceVariant else Color.Transparent)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CyanAccent else BorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onEffectSelected(effect.id) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(effect.swatchColor)
                                .border(1.dp, GraphiteBackground, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = effect.name.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) CyanAccent else TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (selectedEffectId != "off") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intensity: ${(intensity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Slider(
                value = intensity,
                onValueChange = onIntensityChanged,
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reduced motion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Settles effect animations instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = reducedMotion,
                    onCheckedChange = onReducedMotionChanged,
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
}
