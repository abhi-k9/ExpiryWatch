package io.github.abhik9.expirywatch.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.model.Item

/**
 * The product photo when there is one (e.g. from Open Food Facts), otherwise the category emoji.
 */
@Composable
fun ItemThumbnail(
    item: Item,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    var imageFailed by remember(item.imageUrl) { mutableStateOf(false) }
    val imageUrl = item.imageUrl
    if (imageUrl != null && !imageFailed) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            onError = { imageFailed = true },
        )
    } else {
        EmojiAvatar(
            emoji = item.category?.emoji ?: DEFAULT_EMOJI,
            modifier = modifier,
            size = size,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}

private const val DEFAULT_EMOJI = "📦"
