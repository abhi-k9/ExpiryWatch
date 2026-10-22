package io.github.abhik9.expirywatch.feature.widget

import io.github.abhik9.expirywatch.core.domain.usecase.ExpirySummary
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class WidgetFilterTest {
    private val today = LocalDate.of(2026, 3, 10)
    private val camping = CustomGroup(id = 7, name = "Camping kit", emoji = "⛺")
    private val baby = CustomGroup(id = 8, name = "Baby", emoji = "🍼")

    private fun entry(id: Long, name: String, status: ExpiryStatus, group: CustomGroup? = null) = ItemWithExpiry(
        item = Item(id = id, name = name, expiryDate = today, customGroup = group),
        status = status,
        daysUntilExpiry = 0,
    )

    private val summary = ExpirySummary(
        items = listOf(
            entry(1, "Yogurt", ExpiryStatus.EXPIRED),
            entry(2, "Milk", ExpiryStatus.EXPIRING_SOON),
            entry(3, "Beans", ExpiryStatus.FRESH, group = camping),
        ),
        today = today,
    )

    private fun List<ItemWithExpiry>.names() = map { it.item.name }

    @Test
    fun listsAllItemsWhatNeedsAttentionOrAGroups() {
        assertEquals(listOf("Yogurt", "Milk", "Beans"), WidgetFilter.All.apply(summary).names())
        assertEquals(listOf("Yogurt", "Milk"), WidgetFilter.NeedsAttention.apply(summary).names())
        assertEquals(listOf("Beans"), WidgetFilter.Group(camping.id).apply(summary).names())
    }

    @Test
    fun tappingGoesThroughEveryFilterAndBackToAll() {
        val groups = listOf(camping, baby)
        val seen = generateSequence<WidgetFilter>(WidgetFilter.All) { it.next(groups) }.take(5).toList()

        assertEquals(
            listOf(
                WidgetFilter.All,
                WidgetFilter.NeedsAttention,
                WidgetFilter.Group(camping.id),
                WidgetFilter.Group(baby.id),
                WidgetFilter.All,
            ),
            seen,
        )
    }

    @Test
    fun readsBackWhatItSavedAndFallsBackToAllForADeletedGroup() {
        val groups = listOf(camping)
        listOf(WidgetFilter.All, WidgetFilter.NeedsAttention, WidgetFilter.Group(camping.id)).forEach {
            assertEquals(it, WidgetFilter.decode(it.encode(), groups))
        }
        assertEquals(WidgetFilter.All, WidgetFilter.decode(WidgetFilter.Group(baby.id).encode(), groups))
        assertEquals(WidgetFilter.All, WidgetFilter.decode(null, groups))
        assertEquals(WidgetFilter.All, WidgetFilter.decode("group:not-a-number", groups))
    }
}
