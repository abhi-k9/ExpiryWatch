package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Item
import java.time.LocalDate
import javax.inject.Inject

/** How long to leave items out of reminders. */
enum class SnoozeLength {
    ONE_WEEK,
    ONE_MONTH,

    /** Until the user turns the reminders back on. */
    INDEFINITELY,

    ;

    /** The day reminders start again, when snoozed on [today]. */
    fun resumeDate(today: LocalDate): LocalDate = when (this) {
        ONE_WEEK -> today.plusWeeks(1)
        ONE_MONTH -> today.plusMonths(1)
        INDEFINITELY -> Item.SNOOZED_INDEFINITELY
    }
}

/** Leaves items out of the daily reminder for a while, or includes them again. */
class SnoozeRemindersUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    /** Snoozes the reminders about [items] until [until], or turns them back on when it's `null`. */
    suspend operator fun invoke(items: List<Item>, until: LocalDate?) {
        itemRepository.snoozeReminders(items.map { it.id }, until)
    }

    /** Puts the reminders about each of [items] back to how they were, to undo a change. */
    suspend fun restore(items: List<Item>) {
        items.groupBy({ it.remindersSnoozedUntil }, { it.id })
            .forEach { (until, ids) -> itemRepository.snoozeReminders(ids, until) }
    }
}
