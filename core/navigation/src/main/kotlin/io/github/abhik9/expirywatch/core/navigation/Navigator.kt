package io.github.abhik9.expirywatch.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink

/**
 * Moves around a single back stack whose bottom entry is always the item list. The other
 * top-level screens (insights, settings) sit directly on top of it, so Back from them returns to
 * the list, as users expect.
 */
class Navigator(private val backStack: MutableList<NavKey>) {
    init {
        if (backStack.isEmpty()) backStack.add(ItemsNavKey())
    }

    val currentKey: NavKey get() = backStack.last()

    /** The top-level screen the user is in: the last list, insights or settings key on the stack. */
    val currentTopLevelKey: NavKey get() = backStack.last { isTopLevel(it) }

    fun navigate(key: NavKey) {
        if (isTopLevel(key)) {
            navigateToTopLevel(key)
        } else {
            backStack.add(key)
        }
    }

    /** Returns false when there is nothing to go back to, so the caller can finish the app. */
    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun handleDeepLink(deepLink: DeepLink) {
        when (deepLink) {
            is DeepLink.Items -> resetTo(ItemsNavKey(deepLink.status))
            is DeepLink.EditItem -> resetTo(ItemsNavKey(), EditorNavKey(itemId = deepLink.itemId))
            is DeepLink.AddItem -> resetTo(ItemsNavKey(), EditorNavKey(scanBarcode = deepLink.scanBarcode))
        }
    }

    private fun navigateToTopLevel(key: NavKey) {
        // Re-selecting the list clears any filter it was opened with, like re-tapping a tab.
        val root = backStack.first() as? ItemsNavKey ?: ItemsNavKey()
        when {
            key is ItemsNavKey -> resetTo(if (currentTopLevelKey is ItemsNavKey) ItemsNavKey() else root)
            else -> resetTo(root, key)
        }
    }

    private fun resetTo(vararg keys: NavKey) {
        backStack.clear()
        backStack.addAll(keys)
    }

    companion object {
        fun isTopLevel(key: NavKey): Boolean = key is ItemsNavKey || key == InsightsNavKey || key == SettingsNavKey
    }
}
