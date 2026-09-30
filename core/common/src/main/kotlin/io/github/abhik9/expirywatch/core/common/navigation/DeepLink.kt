package io.github.abhik9.expirywatch.core.common.navigation

import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import java.net.URI

/**
 * Entry points into the app from outside of it: notifications, the home-screen widget and
 * launcher shortcuts. Each has a stable URI of the form `expirywatch://…`, so they can be carried
 * by an Intent without depending on the app's navigation code.
 */
sealed interface DeepLink {
    /** The item list, optionally filtered by [status]. */
    data class Items(val status: ExpiryStatus? = null) : DeepLink

    /** The editor for an existing item. */
    data class EditItem(val itemId: Long) : DeepLink

    /** The editor for a new item, optionally opening the barcode scanner straight away. */
    data class AddItem(val scanBarcode: Boolean = false) : DeepLink

    fun toUri(): String = when (this) {
        is Items -> buildString {
            append("$SCHEME://$HOST_ITEMS")
            if (status != null) append("?$PARAM_STATUS=${status.toParam()}")
        }

        is EditItem -> "$SCHEME://$HOST_ITEMS/$itemId"

        is AddItem -> if (scanBarcode) "$SCHEME://$HOST_ADD?$PARAM_SCAN=true" else "$SCHEME://$HOST_ADD"
    }

    companion object {
        const val SCHEME = "expirywatch"
        private const val HOST_ITEMS = "items"
        private const val HOST_ADD = "add"
        private const val PARAM_STATUS = "status"
        private const val PARAM_SCAN = "scan"

        /** Parses a URI produced by [toUri]. Returns `null` for anything unrecognised. */
        fun parse(uri: String): DeepLink? {
            val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
            if (parsed.scheme != SCHEME) return null
            val params = parsed.rawQuery.orEmpty()
                .split('&')
                .filter { it.isNotEmpty() }
                .associate { it.substringBefore('=') to it.substringAfter('=', missingDelimiterValue = "") }
            val pathSegments = parsed.path.orEmpty().split('/').filter { it.isNotEmpty() }
            return when (parsed.host) {
                HOST_ITEMS -> when (pathSegments.size) {
                    0 -> Items(status = params[PARAM_STATUS]?.let(::statusFromParam))
                    1 -> pathSegments[0].toLongOrNull()?.let(::EditItem)
                    else -> null
                }

                HOST_ADD -> AddItem(scanBarcode = params[PARAM_SCAN] == "true")

                else -> null
            }
        }

        private fun ExpiryStatus.toParam(): String = name.lowercase()

        private fun statusFromParam(value: String): ExpiryStatus? =
            ExpiryStatus.entries.firstOrNull { it.toParam() == value }
    }
}
