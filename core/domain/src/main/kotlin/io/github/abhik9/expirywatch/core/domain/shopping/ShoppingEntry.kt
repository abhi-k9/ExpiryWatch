package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.usecase.ItemValidator
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation

/** Something to add to a shopping list, typed in or taken from elsewhere in the app. */
data class ShoppingEntry(
    val name: String,
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val category: Category? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val location: StorageLocation? = null,
    val note: String? = null,
) {
    companion object {
        private val whitespace = "\\s+".toRegex()
        private val leadingNumber = """^(\d+(?:[.,]\d+)?)(\s*)(\S.*)$""".toRegex()
        private val trailingCount = """^(.+?)\s+[x×]\s?(\d+)$""".toRegex(RegexOption.IGNORE_CASE)

        private val units = mapOf(
            QuantityUnit.PIECES to listOf("x", "×", "pc", "pcs", "piece", "pieces"),
            QuantityUnit.PACKS to listOf("pk", "pkg", "pack", "packs", "packet", "packets"),
            QuantityUnit.GRAMS to listOf("g", "gr", "gram", "grams"),
            QuantityUnit.KILOGRAMS to listOf("kg", "kgs", "kilo", "kilos", "kilogram", "kilograms"),
            QuantityUnit.MILLILITERS to listOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"),
            QuantityUnit.LITERS to listOf("l", "lt", "ltr", "liter", "liters", "litre", "litres"),
        ).flatMap { (unit, words) -> words.map { it to unit } }.toMap()

        /**
         * Reads what's typed into the shopping list: a quantity and unit before the name, as in
         * "2 milk", "1.5 kg flour", "500g butter" or "3 packs of pasta", or a count after it, as in
         * "eggs x12".
         * Anything else is all name, so "7up" stays "7up". Returns `null` for blank text.
         */
        fun parse(text: String): ShoppingEntry? {
            val trimmed = text.trim().replace(whitespace, " ")
            if (trimmed.isEmpty()) return null
            return parseLeadingQuantity(trimmed) ?: parseTrailingCount(trimmed) ?: ShoppingEntry(trimmed)
        }

        private fun parseLeadingQuantity(text: String): ShoppingEntry? {
            val match = leadingNumber.matchEntire(text) ?: return null
            val (number, space, rest) = match.destructured
            val quantity = number.replace(',', '.').toDoubleOrNull()?.takeIf(::isValidShoppingQuantity) ?: return null
            val firstWord = rest.substringBefore(' ')
            val unit = units[firstWord.lowercase().removeSuffix(".")]
            val afterUnit = rest.substringAfter(' ', missingDelimiterValue = "").removePrefix("of ").trim()
            return when {
                // "500g butter", "2 kg flour", "3 packs of pasta"
                unit != null && afterUnit.isNotEmpty() -> ShoppingEntry(afterUnit, quantity, unit)

                // "7up" or "3D glasses": the number is part of the name.
                space.isEmpty() -> null

                else -> ShoppingEntry(rest, quantity)
            }
        }

        private fun parseTrailingCount(text: String): ShoppingEntry? {
            val (name, count) = trailingCount.matchEntire(text)?.destructured ?: return null
            val quantity = count.toDoubleOrNull()?.takeIf(::isValidShoppingQuantity) ?: return null
            return ShoppingEntry(name.trim(), quantity)
        }
    }
}

/** Whether [quantity] can be the amount of something to buy. */
fun isValidShoppingQuantity(quantity: Double): Boolean =
    quantity.isFinite() && quantity > 0 && quantity <= ItemValidator.MAX_QUANTITY
