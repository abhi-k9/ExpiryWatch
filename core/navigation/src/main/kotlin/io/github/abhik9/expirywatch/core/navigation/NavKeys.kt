package io.github.abhik9.expirywatch.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import kotlinx.serialization.Serializable

/** The item list, optionally opened with a status filter applied. */
@Serializable
data class ItemsNavKey(val status: ExpiryStatus? = null) : NavKey

@Serializable
data object ShoppingNavKey : NavKey

/** Turns what's in the cart of a shopping list into tracked items. */
@Serializable
data class PutAwayNavKey(val listId: Long) : NavKey

/** Adds the items written in [text] to a shopping list, such as a list shared from another app. */
@Serializable
data class AddFromTextNavKey(val text: String = "") : NavKey

@Serializable
data object InsightsNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

/**
 * The add/edit screen.
 *
 * @property itemId the item to edit, or `null` to add a new one.
 * @property scanBarcode open the barcode scanner right away (only for new items).
 */
@Serializable
data class EditorNavKey(
    val itemId: Long? = null,
    val scanBarcode: Boolean = false,
) : NavKey

/** Settings most people never need to change. */
@Serializable
data object AdvancedSettingsNavKey : NavKey

/** Lets the user rename, add and delete categories, storage locations or their groups. */
@Serializable
data class ManageLabelsNavKey(val kind: LabelKind) : NavKey

enum class LabelKind {
    CATEGORIES,
    LOCATIONS,
    GROUPS,
}
