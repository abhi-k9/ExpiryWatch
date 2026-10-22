package io.github.abhik9.expirywatch.core.notifications

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeRemindersUseCase
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** What the buttons on the reminder notifications do; each brings the reminder up to date after. */
internal class ReminderActions @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val getExpirySummary: GetExpirySummaryUseCase,
    private val itemRepository: ItemRepository,
    private val finishItem: FinishItemUseCase,
    private val snoozeReminders: SnoozeRemindersUseCase,
    private val notifier: ExpiryNotifier,
    private val clock: Clock,
    private val log: EventLog,
) {
    /** Runs the [action] the user picked, for the item with [itemId] unless it's [ACTION_SNOOZE_ALL]. */
    suspend operator fun invoke(action: String, itemId: Long?) {
        when (action) {
            ACTION_SNOOZE_ALL -> snoozeAll()
            ACTION_USED_UP -> itemId?.let { finish(it, ItemStatus.CONSUMED) }
            ACTION_THROWN_AWAY -> itemId?.let { finish(it, ItemStatus.WASTED) }
            ACTION_SNOOZE -> itemId?.let { snooze(it) }
            else -> return
        }
        notifier.updateExpiryReminder(currentSummary())
    }

    /** Snoozes everything the reminder is about for a week. */
    private suspend fun snoozeAll() {
        val items = currentSummary().items.map { it.item }
        snoozeReminders(items, snoozeEnd())
        log.record { "reminder action: snoozed ${items.size} items" }
    }

    private suspend fun finish(itemId: Long, outcome: ItemStatus) {
        finishItem(itemId, outcome)
        notifier.cancelItemReminder(itemId)
        log.record { "reminder action: item $itemId ${outcome.name.lowercase()}" }
    }

    private suspend fun snooze(itemId: Long) {
        itemRepository.observeItem(itemId).first()?.let { snoozeReminders(listOf(it), snoozeEnd()) }
        notifier.cancelItemReminder(itemId)
        log.record { "reminder action: item $itemId snoozed" }
    }

    private fun snoozeEnd(): LocalDate = SnoozeLength.ONE_WEEK.resumeDate(LocalDate.now(clock))

    private suspend fun currentSummary() =
        getExpirySummary().forReminder(settingsRepository.settings.first().remindAboutExpired)

    companion object {
        const val ACTION_SNOOZE_ALL = "io.github.abhik9.expirywatch.action.SNOOZE_ALL"
        const val ACTION_USED_UP = "io.github.abhik9.expirywatch.action.USED_UP"
        const val ACTION_THROWN_AWAY = "io.github.abhik9.expirywatch.action.THROWN_AWAY"
        const val ACTION_SNOOZE = "io.github.abhik9.expirywatch.action.SNOOZE"

        private const val DATA_PREFIX = "expirywatch-action://item/"

        /** Data that tells the actions on different items apart, which extras don't. */
        fun dataUri(itemId: Long?): String = DATA_PREFIX + (itemId?.toString() ?: "all")

        /** The item id in a [dataUri], or `null` for the one without. */
        fun itemIdFrom(dataUri: String?): Long? = dataUri?.removePrefix(DATA_PREFIX)?.toLongOrNull()
    }
}
