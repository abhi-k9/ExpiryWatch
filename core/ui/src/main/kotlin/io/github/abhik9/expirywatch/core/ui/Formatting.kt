package io.github.abhik9.expirywatch.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    val number = formatNumber(quantity)
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

/** Formats a number for the current locale, without a trailing ".0" for whole numbers. */
fun formatNumber(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
    isGroupingUsed = false
}.format(value)

private val mediumDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** A date in the user's locale, e.g. "12 Mar 2026". */
fun LocalDate.formatMedium(): String = mediumDateFormatter.format(this)
