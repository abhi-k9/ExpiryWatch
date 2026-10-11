package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.effectiveExpiryDate
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Why something might need buying again. */
enum class RestockReason {
    /** What's in stock has expired. */
    EXPIRED,

    /** The last of it was used up. */
    USED_UP,

    /** The last of it was thrown away. */
    THROWN_AWAY,
}

/**
 * Something the user might need to buy again.
 *
 * @property since the day it expired, or was used up or thrown away.
 * @property daysAgo how many days before today [since] is.
 */
data class RestockSuggestion(
    val name: String,
    val category: Category?,
    val reason: RestockReason,
    val since: LocalDate,
    val daysAgo: Long,
) {
    /** Identifies the suggestion, so dismissing it hides it until the product runs out again. */
    val key: String get() = "${productKey(name)}$KEY_SEPARATOR$since"
}

private const val KEY_SEPARATOR = '|'

/**
 * Products the user might need to buy again, the most recent first: those whose items in stock
 * have all expired, and those used up or thrown away in the last two weeks with none left. Products
 * on a shopping list, or added to one since they ran out, are left out, as are dismissed
 * suggestions.
 */
class ObserveRestockSuggestionsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
    private val observeToday: ObserveTodayUseCase,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<RestockSuggestion>> = observeToday().flatMapLatest { today ->
        combine(
            itemRepository.observeActiveItems(),
            itemRepository.observeFinishedItems(since = today.minusDays(RECENT_DAYS)),
            shoppingRepository.observeItemNames(),
            shoppingRepository.observeHistory(),
            settingsRepository.settings.map { it.dismissedRestockSuggestions }.distinctUntilChanged(),
        ) { active, finished, onLists, history, dismissed ->
            suggestRestock(
                items = active + finished,
                onLists = onLists.mapTo(HashSet(), ::productKey),
                history = history,
                dismissed = dismissed,
                today = today,
                zone = clock.zone,
            )
        }
    }

    companion object {
        /** How long a product used up or thrown away is suggested for. */
        const val RECENT_DAYS = 14L
    }
}

internal fun suggestRestock(
    items: List<Item>,
    onLists: Set<String>,
    history: List<ShoppingHistoryEntry>,
    dismissed: Set<String>,
    today: LocalDate,
    zone: ZoneId,
): List<RestockSuggestion> {
    val lastAdded = history.associate { productKey(it.name) to it.lastAddedAt.atZone(zone).toLocalDate() }
    return items
        .groupBy { productKey(it.name) }
        .mapNotNull { (key, ofProduct) ->
            if (key.isEmpty() || key in onLists) return@mapNotNull null
            ofProduct.restockSuggestion(today)
                ?.takeIf { suggestion -> lastAdded[key]?.let { it >= suggestion.since } != true }
                ?.takeIf { it.key !in dismissed }
        }
        .sortedWith(compareByDescending<RestockSuggestion> { it.since }.thenBy { productKey(it.name) })
}

/** What to suggest for the items of one product, if anything. */
private fun List<Item>.restockSuggestion(today: LocalDate): RestockSuggestion? {
    val (inStock, gone) = partition { it.status == ItemStatus.ACTIVE }
    val expired = inStock.filter { it.effectiveExpiryDate < today }
    // Some is still good to use.
    if (expired.size < inStock.size) return null
    val latestExpired = expired.maxByOrNull { it.effectiveExpiryDate }
    if (latestExpired != null) {
        val since = latestExpired.effectiveExpiryDate
        return RestockSuggestion(
            name = latestExpired.name.trim(),
            category = latestExpired.category,
            reason = RestockReason.EXPIRED,
            since = since,
            daysAgo = ChronoUnit.DAYS.between(since, today),
        )
    }
    val last = gone.filter { it.finishedDate != null }.maxByOrNull { checkNotNull(it.finishedDate) } ?: return null
    val since = checkNotNull(last.finishedDate)
    return RestockSuggestion(
        name = last.name.trim(),
        category = last.category,
        reason = if (last.status == ItemStatus.WASTED) RestockReason.THROWN_AWAY else RestockReason.USED_UP,
        since = since,
        daysAgo = ChronoUnit.DAYS.between(since, today),
    )
}

/**
 * Hides a restock suggestion until the product runs out again. Dismissals of things that ran
 * out over a year ago are forgotten, so they don't pile up.
 */
class DismissRestockSuggestionUseCase @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(suggestion: RestockSuggestion) {
        val forgetBefore = LocalDate.now(clock).minusDays(FORGET_AFTER_DAYS)
        val kept = settingsRepository.settings.first().dismissedRestockSuggestions.filterTo(HashSet()) { key ->
            val since = runCatching { LocalDate.parse(key.substringAfterLast(KEY_SEPARATOR)) }.getOrNull()
            since != null && since >= forgetBefore
        }
        settingsRepository.setDismissedRestockSuggestions(kept + suggestion.key)
    }

    private companion object {
        const val FORGET_AFTER_DAYS = 365L
    }
}
