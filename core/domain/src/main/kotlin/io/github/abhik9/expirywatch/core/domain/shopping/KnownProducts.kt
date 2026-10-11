package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.model.Category
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * A product the user has bought or kept before, to suggest while they add to a shopping list.
 *
 * @property timesAdded how often it was added to a shopping list; 0 when it's only known as an item.
 */
data class KnownProduct(
    val name: String,
    val category: Category? = null,
    val timesAdded: Int = 0,
) {
    val key: String = productKey(name)
}

/**
 * The products the user knows: what they added to shopping lists before, most often added first,
 * then the items they have or finished in the last year. A product takes the category of the
 * items called the same when it wasn't given one on a shopping list.
 */
class ObserveKnownProductsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val itemRepository: ItemRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<KnownProduct>> = combine(
        shoppingRepository.observeHistory(),
        itemRepository.observeActiveItems(),
        itemRepository.observeFinishedItems(since = LocalDate.now(clock).minusDays(FINISHED_ITEMS_DAYS)),
    ) { history, active, finished ->
        val kept = (active + finished)
            .map { KnownProduct(it.name.trim(), it.category) }
            .sortedWith(compareBy<KnownProduct> { it.key }.thenBy { it.category == null })
            .distinctBy { it.key }
        val categories = kept.associate { it.key to it.category }
        val added = history.map { entry ->
            KnownProduct(entry.name, entry.category, entry.timesAdded).let { product ->
                if (product.category != null) product else product.copy(category = categories[product.key])
            }
        }
        (added + kept).filter { it.key.isNotEmpty() }.distinctBy { it.key }
    }

    private companion object {
        const val FINISHED_ITEMS_DAYS = 365L
    }
}

/**
 * The [known] products to suggest for [query], the name typed so far: those starting with it first,
 * then those with a word starting with it, then those containing it, each in the order of [known].
 * Without a query, the products added to shopping lists most often. Products whose [productKey] is
 * in [exclude], such as the ones already on the list, are left out.
 */
fun suggestProducts(
    query: String,
    known: List<KnownProduct>,
    exclude: Set<String> = emptySet(),
    limit: Int = 8,
): List<KnownProduct> {
    val typed = productKey(query)
    val candidates = known.asSequence().filter { it.key !in exclude }
    if (typed.isEmpty()) return candidates.filter { it.timesAdded > 0 }.take(limit).toList()
    return candidates
        .mapNotNull { product ->
            val rank = when {
                product.key.startsWith(typed) -> 0
                product.key.split(' ').any { it.startsWith(typed) } -> 1
                typed in product.key -> 2
                else -> return@mapNotNull null
            }
            rank to product
        }
        .sortedBy { (rank, _) -> rank }
        .take(limit)
        .map { (_, product) -> product }
        .toList()
}
