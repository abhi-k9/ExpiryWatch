package io.github.abhik9.expirywatch.core.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Human-friendly descriptions of how far away an expiry date is. Usable from Compose and from
 * places without a composition, such as notifications.
 */
object ExpiryText {
    /** A full sentence, e.g. "Expires tomorrow" or "Expired 3 days ago". */
    fun sentence(resources: Resources, daysUntilExpiry: Long): String =
        sentencePhrase(daysUntilExpiry).render(resources)

    /** A compact label for badges, e.g. "Tomorrow", "In 5 days" or "3 days ago". */
    fun short(resources: Resources, daysUntilExpiry: Long): String = shortPhrase(daysUntilExpiry).render(resources)

    internal fun sentencePhrase(daysUntilExpiry: Long): Phrase {
        val days = daysUntilExpiry.toIntClamped()
        return when {
            days < -1 -> Phrase.Plural(R.plurals.core_ui_expired_days_ago, -days)
            days == -1 -> Phrase.Text(R.string.core_ui_expired_yesterday)
            days == 0 -> Phrase.Text(R.string.core_ui_expires_today)
            days == 1 -> Phrase.Text(R.string.core_ui_expires_tomorrow)
            else -> Phrase.Plural(R.plurals.core_ui_expires_in_days, days)
        }
    }

    internal fun shortPhrase(daysUntilExpiry: Long): Phrase {
        val days = daysUntilExpiry.toIntClamped()
        return when {
            days < -1 -> Phrase.Plural(R.plurals.core_ui_days_ago_short, -days)
            days == -1 -> Phrase.Text(R.string.core_ui_yesterday)
            days == 0 -> Phrase.Text(R.string.core_ui_today)
            days == 1 -> Phrase.Text(R.string.core_ui_tomorrow)
            else -> Phrase.Plural(R.plurals.core_ui_in_days_short, days)
        }
    }

    private fun Long.toIntClamped(): Int = coerceIn(-MAX_DAYS, MAX_DAYS).toInt()

    private const val MAX_DAYS = 99_999L

    internal sealed interface Phrase {
        data class Text(@param:StringRes val id: Int) : Phrase

        /** A plural whose only format argument is its [count]. */
        data class Plural(@param:PluralsRes val id: Int, val count: Int) : Phrase

        fun render(resources: Resources): String = when (this) {
            is Text -> resources.getString(id)
            is Plural -> resources.getQuantityString(id, count, count)
        }
    }
}

@Composable
private fun ExpiryText.Phrase.render(): String = when (this) {
    is ExpiryText.Phrase.Text -> stringResource(id)
    is ExpiryText.Phrase.Plural -> pluralStringResource(id, count, count)
}

@Composable
fun expirySentence(daysUntilExpiry: Long): String = ExpiryText.sentencePhrase(daysUntilExpiry).render()

@Composable
fun expiryShortLabel(daysUntilExpiry: Long): String = ExpiryText.shortPhrase(daysUntilExpiry).render()
