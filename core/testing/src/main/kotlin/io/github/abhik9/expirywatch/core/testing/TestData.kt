package io.github.abhik9.expirywatch.core.testing

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A fixed point in time shared by tests: 2026-03-10, 12:00 UTC. */
object TestTime {
    val today: LocalDate = LocalDate.of(2026, 3, 10)
    val now: Instant = today.atTime(12, 0).toInstant(ZoneOffset.UTC)
    val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
}

object TestData {
    val dairy = Category(id = 1, name = "Dairy", emoji = "🥛")
    val produce = Category(id = 2, name = "Produce", emoji = "🥦")
    val fridge = StorageLocation(id = 1, name = "Fridge", emoji = "🧊")
    val pantry = StorageLocation(id = 2, name = "Pantry", emoji = "🥫")

    fun item(
        id: Long = 0,
        name: String = "Milk",
        expiresInDays: Long = 5,
        category: Category? = dairy,
        location: StorageLocation? = fridge,
        today: LocalDate = TestTime.today,
    ) = Item(
        id = id,
        name = name,
        category = category,
        location = location,
        expiryDate = today.plusDays(expiresInDays),
        createdAt = TestTime.now,
        updatedAt = TestTime.now,
    )
}
