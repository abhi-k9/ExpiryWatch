package io.github.abhik9.expirywatch.feature.widget

import io.github.abhik9.expirywatch.core.domain.usecase.ExpirySummary
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.CustomGroup

/** Which items a widget lists; each placed widget keeps its own. */
internal sealed interface WidgetFilter {
    data object All : WidgetFilter

    /** The expired items and those expiring soon. */
    data object NeedsAttention : WidgetFilter

    data class Group(val groupId: Long) : WidgetFilter

    fun apply(summary: ExpirySummary): List<ItemWithExpiry> = when (this) {
        All -> summary.items
        NeedsAttention -> summary.expired + summary.expiringSoon
        is Group -> summary.items.filter { it.item.customGroup?.id == groupId }
    }

    /** The filter after this one when tapped: all, then what needs attention, then each of the [groups]. */
    fun next(groups: List<CustomGroup>): WidgetFilter {
        val all = listOf(All, NeedsAttention) + groups.map { Group(it.id) }
        return all.getOrNull(all.indexOf(this) + 1) ?: All
    }

    fun encode(): String = when (this) {
        All -> ALL
        NeedsAttention -> NEEDS_ATTENTION
        is Group -> "$GROUP_PREFIX$groupId"
    }

    companion object {
        private const val ALL = "all"
        private const val NEEDS_ATTENTION = "attention"
        private const val GROUP_PREFIX = "group:"

        /** The filter [encoded], or all items for anything unknown, like a group since deleted. */
        fun decode(encoded: String?, groups: List<CustomGroup>): WidgetFilter = when {
            encoded == NEEDS_ATTENTION -> NeedsAttention

            encoded != null && encoded.startsWith(GROUP_PREFIX) ->
                encoded.removePrefix(GROUP_PREFIX).toLongOrNull()
                    ?.takeIf { id -> groups.any { it.id == id } }
                    ?.let(::Group)
                    ?: All

            else -> All
        }
    }
}
