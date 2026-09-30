package io.github.abhik9.expirywatch.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.R
import io.github.abhik9.expirywatch.core.navigation.InsightsNavKey
import io.github.abhik9.expirywatch.core.navigation.ItemsNavKey
import io.github.abhik9.expirywatch.core.navigation.SettingsNavKey

/** The screens reachable from the navigation bar. */
enum class TopLevelDestination(
    val key: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val label: Int,
) {
    ITEMS(ItemsNavKey(), Icons.Filled.Inventory2, Icons.Outlined.Inventory2, R.string.nav_items),
    INSIGHTS(InsightsNavKey, Icons.Filled.Insights, Icons.Outlined.Insights, R.string.nav_insights),
    SETTINGS(SettingsNavKey, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.nav_settings),
    ;

    /** The item list is one destination whatever filter it was opened with. */
    fun matches(key: NavKey): Boolean = if (this == ITEMS) key is ItemsNavKey else key == this.key
}
