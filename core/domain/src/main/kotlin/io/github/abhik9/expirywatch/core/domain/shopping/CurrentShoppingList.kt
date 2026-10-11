package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ShoppingList
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The name of the shopping list made when there isn't one yet, in the user's language. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultShoppingListName

/** A shopping list with how many items are still to buy on it. */
data class ShoppingListSummary(
    val list: ShoppingList,
    val toBuyCount: Int,
)

/**
 * The shopping list to use: the one opened last, or the first one when that's gone. One is made
 * when there isn't any yet.
 */
class GetShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
    @DefaultShoppingListName private val defaultName: String,
    private val clock: Clock,
) {
    suspend operator fun invoke(): ShoppingList = creating.withLock {
        val lists = shoppingRepository.getLists()
        val openedLast = settingsRepository.settings.first().shoppingListId
        lists.firstOrNull { it.id == openedLast } ?: lists.firstOrNull() ?: run {
            val createdAt = Instant.now(clock)
            val id = shoppingRepository.createList(defaultName, createdAt)
            ShoppingList(id = id, name = defaultName, createdAt = createdAt)
        }
    }

    private companion object {
        // Shared by every instance, so two screens asking at once don't make two lists.
        val creating = Mutex()
    }
}

/** The shopping list to use, as it changes; see [GetShoppingListUseCase]. */
class ObserveShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
    private val getShoppingList: GetShoppingListUseCase,
) {
    operator fun invoke(): Flow<ShoppingList> = combine(
        shoppingRepository.observeLists(),
        settingsRepository.settings.map { it.shoppingListId }.distinctUntilChanged(),
    ) { lists, openedLast ->
        lists.firstOrNull { it.id == openedLast } ?: lists.firstOrNull() ?: getShoppingList()
    }.distinctUntilChanged()
}

/** Every shopping list, oldest first, with how much is left to buy on each. */
class ObserveShoppingListSummariesUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    operator fun invoke(): Flow<List<ShoppingListSummary>> = combine(
        shoppingRepository.observeLists(),
        shoppingRepository.observeToBuyCounts(),
    ) { lists, counts ->
        lists.map { ShoppingListSummary(it, counts[it.id] ?: 0) }
    }
}
