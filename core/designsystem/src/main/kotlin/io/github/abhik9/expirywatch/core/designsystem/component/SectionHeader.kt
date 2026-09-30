package io.github.abhik9.expirywatch.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = color,
    )
}
