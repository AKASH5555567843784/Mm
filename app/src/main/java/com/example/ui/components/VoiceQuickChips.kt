package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary

@Composable
fun VoiceQuickChips(
    suggestions: List<String>,
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEachIndexed { index, text ->
            AssistChip(
                onClick = { onChipSelected(text) },
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = if (index % 2 == 0) NeonMagenta else NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = DarkSurfaceVariant.copy(alpha = 0.9f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = NeonCyan.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("quick_chip_$index")
            )
        }
    }
}
