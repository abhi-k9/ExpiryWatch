package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.CustomGroupRepository
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** That a category, location or group was given to an item, and when: when the item was last saved. */
data class LabelUse(val labelId: Long, val at: Instant)

/** How many of the most often used labels lead the list, before the most recently used ones. */
const val MOST_OFTEN_USED_COUNT = 2

/** How many of the most recently used labels follow the most often used ones. */
const val MOST_RECENTLY_USED_COUNT = 3

/**
 * Orders [labels], given sorted by name, as they're offered when picking one: the
 * [MOST_OFTEN_USED_COUNT] used most often, then the [MOST_RECENTLY_USED_COUNT] of the others used
 * most recently, then the rest by name. Labels that were never used only come in the rest.
 */
fun <T> orderByUse(labels: List<T>, uses: List<LabelUse>, id: (T) -> Long): List<T> {
    val counts = uses.groupingBy { it.labelId }.eachCount()
    val lastUsed = uses.groupBy { it.labelId }.mapValues { (_, all) -> all.maxOf { it.at } }
    val mostOften = labels
        .filter { id(it) in counts }
        .sortedWith(compareByDescending<T> { counts.getValue(id(it)) }.thenByDescending { lastUsed.getValue(id(it)) })
        .take(MOST_OFTEN_USED_COUNT)
    val mostRecent = labels
        .filter { id(it) in lastUsed && it !in mostOften }
        .sortedByDescending { lastUsed.getValue(id(it)) }
        .take(MOST_RECENTLY_USED_COUNT)
    val top = mostOften + mostRecent
    return top + labels.filterNot { it in top }
}

/** Categories, locations and groups, each ordered by [orderByUse]. */
data class LabelsByUse(
    val categories: List<Category> = emptyList(),
    val locations: List<StorageLocation> = emptyList(),
    val groups: List<CustomGroup> = emptyList(),
)

/**
 * The labels to pick from for an item, the ones used most and lately first. Uses are counted on
 * the items in stock and those finished in the last year.
 */
class ObserveLabelsByUseUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
    private val groupRepository: CustomGroupRepository,
    private val itemRepository: ItemRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<LabelsByUse> {
        val items = combine(
            itemRepository.observeActiveItems(),
            itemRepository.observeFinishedItems(since = LocalDate.now(clock).minusDays(LOOK_BACK_DAYS)),
        ) { active, finished -> active + finished }
        return combine(
            categoryRepository.observeCategories(),
            locationRepository.observeLocations(),
            groupRepository.observeGroups(),
            items,
        ) { categories, locations, groups, all ->
            LabelsByUse(
                categories = orderByUse(categories, all.usesOf { it.category?.id }) { it.id },
                locations = orderByUse(locations, all.usesOf { it.location?.id }) { it.id },
                groups = orderByUse(groups, all.usesOf { it.customGroup?.id }) { it.id },
            )
        }
    }

    private fun List<Item>.usesOf(label: (Item) -> Long?): List<LabelUse> =
        mapNotNull { item -> label(item)?.let { LabelUse(it, item.updatedAt) } }

    private companion object {
        const val LOOK_BACK_DAYS = 365L
    }
}
