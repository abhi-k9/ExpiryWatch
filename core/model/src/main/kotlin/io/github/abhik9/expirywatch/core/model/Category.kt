package io.github.abhik9.expirywatch.core.model

/** A kind of item, such as "Dairy" or "Medicine". */
data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
)

/** Where an item is kept, such as "Fridge" or "Pantry". */
data class StorageLocation(
    val id: Long = 0,
    val name: String,
    val emoji: String,
)

/**
 * A group the user makes to keep items together, such as "Camping kit" or "Baby", whatever their
 * category. The list shows a group's items folded into one row.
 */
data class CustomGroup(
    val id: Long = 0,
    val name: String,
    val emoji: String,
)
