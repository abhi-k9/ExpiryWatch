package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.daysUntilExpiry
import io.github.abhik9.expirywatch.core.model.effectiveExpiryDate
import io.github.abhik9.expirywatch.core.model.expiryStatus
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Filters applied to the item list. `null` means "any". */
data class ItemQuery(
    val searchText: String = "",
    val status: ExpiryStatus? = null,
    val categoryId: Long? = null,
    val locationId: Long? = null,
) {
    val hasFilters: Boolean
        get() = searchText.isNotBlank() || status != null || categoryId != null || locationId != null
}

/** An item together with where it stands on [today][ItemsOverview.today]. */
data class ItemWithExpiry(
    val item: Item,
    val status: ExpiryStatus,
    val daysUntilExpiry: Long,
)

/**
 * Active items of the same product, such as three cartons of milk that expire on different days.
 * Items are the same product when their names match, ignoring case, accents and spacing.
 *
 * @property key identifies the group within the list.
 * @property entries soonest to expire first, so the first is the one to use next.
 */
data class ItemGroup(
    val key: String,
    val entries: List<ItemWithExpiry>,
) {
    val next: ItemWithExpiry get() = entries.first()
}

/** A part of the list: the items in one [status], or all of them when [status] is `null`. */
data class ItemSection(
    val status: ExpiryStatus?,
    val groups: List<ItemGroup>,
) {
    val itemCount: Int get() = groups.sumOf { it.entries.size }
}

data class ItemsOverview(
    /** Items matching every filter of the query, sorted by [sortOrder]. */
    val items: List<ItemWithExpiry>,
    /**
     * The same items to show in the list: under a header per status when sorted by expiry and
     * not filtered by status, and grouped by product. A group goes where its first item sorts.
     */
    val sections: List<ItemSection>,
    /** How many items match the query in each status, ignoring the query's status filter. */
    val statusCounts: Map<ExpiryStatus, Int>,
    /** All active items, before any filtering. */
    val totalActiveCount: Int,
    val sortOrder: ItemSortOrder,
    val today: LocalDate,
)

/** The user's active items, filtered and sorted, and kept up to date as anything changes. */
class ObserveItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: UserSettingsRepository,
    private val observeToday: ObserveTodayUseCase,
) {
    operator fun invoke(query: Flow<ItemQuery>): Flow<ItemsOverview> = combine(
        itemRepository.observeActiveItems(),
        settingsRepository.settings,
        observeToday(),
        query,
    ) { items, settings, today, currentQuery ->
        buildItemsOverview(
            items = items,
            query = currentQuery,
            sortOrder = settings.sortOrder,
            today = today,
            expiringSoonDays = settings.expiringSoonDays,
        )
    }
}

internal fun buildItemsOverview(
    items: List<Item>,
    query: ItemQuery,
    sortOrder: ItemSortOrder,
    today: LocalDate,
    expiringSoonDays: Int,
): ItemsOverview {
    val searchTerms = query.searchText.normalizedForSearch()
        .split(' ')
        .filter { it.isNotEmpty() }

    val matchingExceptStatus = items
        .asSequence()
        .filter { query.categoryId == null || it.category?.id == query.categoryId }
        .filter { query.locationId == null || it.location?.id == query.locationId }
        .filter { it.matchesAll(searchTerms) }
        .map {
            ItemWithExpiry(
                item = it,
                status = it.expiryStatus(today, expiringSoonDays),
                daysUntilExpiry = it.daysUntilExpiry(today),
            )
        }
        .toList()

    val statusCounts = ExpiryStatus.entries.associateWith { status ->
        matchingExceptStatus.count { it.status == status }
    }

    val visible = matchingExceptStatus
        .filter { query.status == null || it.status == query.status }
        .sortedWith(sortOrder.comparator())

    val sections = if (sortOrder.sectionsByStatus && query.status == null) {
        visible.groupBy { it.status }.map { (status, inStatus) ->
            ItemSection(status, inStatus.groupedByProduct(sectionKey = status.name))
        }
    } else {
        listOf(ItemSection(status = null, groups = visible.groupedByProduct("all")))
    }

    return ItemsOverview(
        items = visible,
        sections = sections,
        statusCounts = statusCounts,
        totalActiveCount = items.size,
        sortOrder = sortOrder,
        today = today,
    )
}

private val ItemSortOrder.sectionsByStatus: Boolean
    get() = this == ItemSortOrder.EXPIRY_SOONEST || this == ItemSortOrder.EXPIRY_LATEST

/** Groups sorted items by product, keeping the order in which each product first appears. */
private fun List<ItemWithExpiry>.groupedByProduct(sectionKey: String): List<ItemGroup> =
    groupBy { it.item.name.productKey() }.map { (product, entries) ->
        ItemGroup(
            key = "$sectionKey/$product",
            entries = entries.sortedWith(
                compareBy<ItemWithExpiry> { it.item.effectiveExpiryDate }.thenBy { it.item.id },
            ),
        )
    }

private val whitespace = "\\s+".toRegex()

/** What identifies a product: its name, ignoring case, accents and spacing. */
internal fun String.productKey(): String = normalizedForSearch().replace(whitespace, " ")

private fun ItemSortOrder.comparator(): Comparator<ItemWithExpiry> {
    val byName = compareBy<ItemWithExpiry> { it.item.name.lowercase(Locale.ROOT) }
    return when (this) {
        ItemSortOrder.EXPIRY_SOONEST ->
            compareBy<ItemWithExpiry> { it.item.effectiveExpiryDate }.then(byName)

        ItemSortOrder.EXPIRY_LATEST ->
            compareByDescending<ItemWithExpiry> { it.item.effectiveExpiryDate }.then(byName)

        ItemSortOrder.NAME ->
            byName.thenBy { it.item.effectiveExpiryDate }

        ItemSortOrder.RECENTLY_ADDED ->
            compareByDescending<ItemWithExpiry> { it.item.createdAt }.thenByDescending { it.item.id }
    }
}

private fun Item.matchesAll(searchTerms: List<String>): Boolean {
    if (searchTerms.isEmpty()) return true
    val text = searchableText()
    return searchTerms.all { it in text }
}

private fun Item.searchableText(): String = listOfNotNull(
    name,
    brand,
    barcode,
    notes,
    category?.name,
    location?.name,
).joinToString(separator = " ").normalizedForSearch()

private val combiningMarks = "\\p{Mn}+".toRegex()

/** Lower-cases and strips accents, so "creme" finds "Crème fraîche". */
internal fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .trim()
