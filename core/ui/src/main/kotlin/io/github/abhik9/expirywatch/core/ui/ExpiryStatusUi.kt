package io.github.abhik9.expirywatch.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.abhik9.expirywatch.core.designsystem.component.Pill
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.designsystem.theme.StatusColor
import io.github.abhik9.expirywatch.core.model.ExpiryStatus

val ExpiryStatus.colors: StatusColor
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        ExpiryStatus.EXPIRED -> ExpiryWatchTheme.statusColors.expired
        ExpiryStatus.EXPIRING_SOON -> ExpiryWatchTheme.statusColors.expiringSoon
        ExpiryStatus.FRESH -> ExpiryWatchTheme.statusColors.fresh
    }

@Composable
fun ExpiryStatus.label(): String = stringResource(
    when (this) {
        ExpiryStatus.EXPIRED -> R.string.core_ui_status_expired
        ExpiryStatus.EXPIRING_SOON -> R.string.core_ui_status_expiring_soon
        ExpiryStatus.FRESH -> R.string.core_ui_status_fresh
    },
)

/** A colored label such as "Tomorrow" or "3 days ago", tinted by [status]. */
@Composable
fun ExpiryBadge(
    status: ExpiryStatus,
    daysUntilExpiry: Long,
    modifier: Modifier = Modifier,
) {
    val colors = status.colors
    Pill(
        text = expiryShortLabel(daysUntilExpiry),
        containerColor = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier,
    )
}
