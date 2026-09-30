package io.github.abhik9.expirywatch

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink

/** Launcher shortcuts shown on a long press of the app icon. */
internal object AppShortcuts {
    fun publish(context: Context) {
        val shortcuts = listOf(
            shortcut(
                context,
                "scan",
                R.string.shortcut_scan,
                R.drawable.ic_shortcut_scan,
                DeepLink.AddItem(scanBarcode = true),
            ),
            shortcut(context, "add", R.string.shortcut_add, R.drawable.ic_shortcut_add, DeepLink.AddItem()),
        )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun shortcut(
        context: Context,
        id: String,
        labelRes: Int,
        iconRes: Int,
        deepLink: DeepLink,
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(context.getString(labelRes))
        .setIcon(IconCompat.createWithResource(context, iconRes))
        .setIntent(Intent(Intent.ACTION_VIEW, deepLink.toUri().toUri()).setPackage(context.packageName))
        .build()
}
