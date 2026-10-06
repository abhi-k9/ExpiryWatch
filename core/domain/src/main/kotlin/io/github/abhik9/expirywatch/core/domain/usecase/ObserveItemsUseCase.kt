package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ProductGrouping
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
 * What counts as the same product is up to the user; see [ProductGrouping].
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

/**
 * A part of the list: the items in one [status], or all of them when [status] is `null`. When
 * products are kept together, a group is in the status of its first item, so it can hold items in
 * other statuses too.
 */
data class ItemSection(
    val status: ExpiryStatus?,
    val groups: List<ItemGroup>,
) {
    /** How many of the section's items are in its status. */
    val itemCount: Int get() = groups.sumOf { group -> group.entries.count { status == null || it.status == status } }
}

data class ItemsOverview(
    /** Items matching every filter of the query, sorted by [sortOrder]. */
    val items: List<ItemWithExpiry>,
    /**
     * The same items to show in the list: under a header per status when sorted by expiry and
     * not filtered by status, and grouped by product. A group goes where its first item sorts, or
     * when products are kept together, where its item that expires first sorts.
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
            grouping = settings.productGrouping,
            keepProductsTogether = settings.keepProductsTogether,
        )
    }
}

internal fun buildItemsOverview(
    items: List<Item>,
    query: ItemQuery,
    sortOrder: ItemSortOrder,
    today: LocalDate,
    expiringSoonDays: Int,
    grouping: ProductGrouping = ProductGrouping.NAME,
    keepProductsTogether: Boolean = false,
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

    val sortComparator = sortOrder.comparator()
    val visible = matchingExceptStatus
        .filter { query.status == null || it.status == query.status }
        .sortedWith(sortComparator)

    val sections = when {
        !sortOrder.sectionsByStatus || query.status != null ->
            listOf(ItemSection(status = null, groups = visible.groupedByProduct(grouping, sectionKey = "all")))

        keepProductsTogether -> visible.groupedByProduct(grouping, sectionKey = "together")
            .sortedWith { a, b -> sortComparator.compare(a.next, b.next) }
            .groupBy { it.next.status }
            .map { (status, groups) -> ItemSection(status, groups) }

        else -> visible.groupBy { it.status }.map { (status, inStatus) ->
            ItemSection(status, inStatus.groupedByProduct(grouping, sectionKey = status.name))
        }
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
private fun List<ItemWithExpiry>.groupedByProduct(grouping: ProductGrouping, sectionKey: String): List<ItemGroup> =
    groupBy { it.item.productKey(grouping) }.map { (product, entries) ->
        ItemGroup(
            key = "$sectionKey/$product",
            entries = entries.sortedWith(
                compareBy<ItemWithExpiry> { it.item.effectiveExpiryDate }.thenBy { it.item.id },
            ),
        )
    }

/** What identifies the item's product, as far as [grouping] is concerned. */
private fun Item.productKey(grouping: ProductGrouping): String = when (grouping) {
    ProductGrouping.OFF -> "#$id"
    ProductGrouping.NAME -> name.comparable()
    ProductGrouping.NAME_AND_BRAND -> "${name.comparable()}\u0000${brand.orEmpty().comparable()}"
}

private val whitespace = "\\s+".toRegex()

/** Ignores case, accents and spacing. */
private fun String.comparable(): String = normalizedForSearch().replace(whitespace, " ")

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
