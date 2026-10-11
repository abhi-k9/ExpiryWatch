package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemResult
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Something bought, about to be put away as a tracked item.
 *
 * @property expiryDate when it expires: guessed from how long the product kept before, or `null`
 * when it can't be.
 * @property location where it goes: where the list says, or else where the product was kept before.
 * @property lastTime the product's most recently added item, whose other details it takes on.
 */
data class PutAwayDraft(
    val bought: ShoppingItem,
    val expiryDate: LocalDate?,
    val location: StorageLocation?,
    val lastTime: Item? = null,
)

/** What's in the cart of a shopping list, ready to put away, with expiry dates guessed where possible. */
class GetPutAwayDraftsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val itemRepository: ItemRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(listId: Long): List<PutAwayDraft> {
        val today = LocalDate.now(clock)
        val earlier = (
            itemRepository.getActiveItems() +
                itemRepository.observeFinishedItems(since = today.minusDays(LOOK_BACK_DAYS)).first()
            ).groupBy { productKey(it.name) }
        return shoppingRepository.getItems(listId)
            .filter { it.isChecked }
            .sortedWith(compareBy<ShoppingItem> { productKey(it.name) }.thenBy { it.id })
            .map { bought ->
                val lastTime = earlier[productKey(bought.name)]?.maxByOrNull { it.createdAt }
                PutAwayDraft(
                    bought = bought,
                    expiryDate = lastTime?.shelfLifeDays(clock.zone)?.let(today::plusDays),
                    location = bought.location ?: lastTime?.location,
                    lastTime = lastTime,
                )
            }
    }

    private companion object {
        const val LOOK_BACK_DAYS = 365L
    }
}

/** How many days the item kept from the day it was added, if that's believable as a shelf life. */
private fun Item.shelfLifeDays(zone: ZoneId): Long? =
    ChronoUnit.DAYS.between(createdAt.atZone(zone).toLocalDate(), expiryDate).takeIf { it in 1..MAX_SHELF_LIFE_DAYS }

private const val MAX_SHELF_LIFE_DAYS = 5 * 365L

/**
 * Puts bought things away: each with an expiry date becomes a tracked item and leaves the shopping
 * list. Returns how many were put away.
 */
class PutAwayUseCase @Inject constructor(
    private val saveItem: SaveItemUseCase,
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(drafts: List<PutAwayDraft>): Int {
        val putAway = drafts.filter { draft ->
            val item = draft.toItem() ?: return@filter false
            saveItem(item) is SaveItemResult.Saved
        }
        shoppingRepository.deleteItems(putAway.map { it.bought.id })
        return putAway.size
    }
}

/** The tracked item [PutAwayDraft.bought] becomes, or `null` without an expiry date. */
private fun PutAwayDraft.toItem(): Item? {
    val expiry = expiryDate ?: return null
    return Item(
        name = bought.name,
        brand = bought.brand ?: lastTime?.brand,
        barcode = bought.barcode ?: lastTime?.barcode,
        category = bought.category ?: lastTime?.category,
        location = location,
        quantity = bought.quantity,
        unit = bought.unit,
        expiryDate = expiry,
        useWithinDaysAfterOpening = lastTime?.useWithinDaysAfterOpening,
        imageUrl = lastTime?.imageUrl,
    )
}
