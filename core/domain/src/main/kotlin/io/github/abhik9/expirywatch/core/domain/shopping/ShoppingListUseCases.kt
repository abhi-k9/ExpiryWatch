package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Makes a shopping list and opens it. Returns its id, or `null` without making one when [name] is blank. */
class CreateShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(name: String): Long? {
        val trimmed = name.trim().ifEmpty { return null }
        return shoppingRepository.createList(trimmed, Instant.now(clock))
            .also { settingsRepository.setShoppingListId(it) }
    }
}

/** Renames a shopping list. Returns false without renaming it when [name] is blank. */
class RenameShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(id: Long, name: String): Boolean {
        val trimmed = name.trim().ifEmpty { return false }
        shoppingRepository.renameList(id, trimmed)
        return true
    }
}

/** Deletes a shopping list with everything on it. When it was open, the first list left opens instead. */
class DeleteShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
) {
    suspend operator fun invoke(id: Long) {
        val wasOpen = settingsRepository.settings.first().shoppingListId == id
        shoppingRepository.deleteList(id)
        if (wasOpen) shoppingRepository.getLists().firstOrNull()?.let { settingsRepository.setShoppingListId(it.id) }
    }
}
