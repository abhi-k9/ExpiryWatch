package io.github.abhik9.expirywatch.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The locale to format text for. Unlike [Locale.getDefault], reading it recomposes the caller when
 * the user changes the app or system language.
 */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** The unit's name as shown in pickers, e.g. "Grams". */
@Composable
fun QuantityUnit.label(): String = stringResource(
    when (this) {
        QuantityUnit.PIECES -> R.string.core_ui_unit_pieces
        QuantityUnit.PACKS -> R.string.core_ui_unit_packs
        QuantityUnit.GRAMS -> R.string.core_ui_unit_grams
        QuantityUnit.KILOGRAMS -> R.string.core_ui_unit_kilograms
        QuantityUnit.MILLILITERS -> R.string.core_ui_unit_milliliters
        QuantityUnit.LITERS -> R.string.core_ui_unit_liters
    },
)

/** A quantity with its unit, e.g. "2 pcs", "1.5 L" or "500 g". */
@Composable
fun formatQuantity(quantity: Double, unit: QuantityUnit): String {
    val number = formatNumber(quantity, currentLocale())
    return stringResource(
        when (unit) {
            QuantityUnit.PIECES -> R.string.core_ui_quantity_pieces
            QuantityUnit.PACKS -> R.string.core_ui_quantity_packs
            QuantityUnit.GRAMS -> R.string.core_ui_quantity_grams
            QuantityUnit.KILOGRAMS -> R.string.core_ui_quantity_kilograms
            QuantityUnit.MILLILITERS -> R.string.core_ui_quantity_milliliters
            QuantityUnit.LITERS -> R.string.core_ui_quantity_liters
        },
        number,
    )
}

/** Formats a number for [locale], without a trailing ".0" for whole numbers. */
fun formatNumber(value: Double, locale: Locale): String = NumberFormat.getNumberInstance(locale).apply {
    maximumFractionDigits = 2
    isGroupingUsed = false
}.format(value)

/** A date in the user's locale, e.g. "12 Mar 2026". */
@Composable
@ReadOnlyComposable
fun LocalDate.formatMedium(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()).format(this)

/** A time of day in the user's locale, following their 12/24-hour preference, e.g. "8:15 PM" or "20:15". */
@Composable
@ReadOnlyComposable
fun LocalTime.formatShort(): String {
    val locale = currentLocale()
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(this)
}
