package io.github.abhik9.expirywatch.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme

/** A round badge showing an emoji, used for categories and storage locations. */
@Composable
fun EmojiAvatar(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (size.value * EMOJI_SCALE).sp)
    }
}

private const val EMOJI_SCALE = 0.45f

@Preview
@Composable
private fun EmojiAvatarPreview() {
    ExpiryWatchTheme(dynamicColor = false) {
        EmojiAvatar(emoji = "🧀")
    }
}
