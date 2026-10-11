package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import java.math.BigDecimal

/** Shopping lists as plain text, to share with other apps and to read back. */
object ShoppingListText {
    /** A list marker at the start of a line: "-", "*", "•", "[ ]", "☐", "1." and the like. */
    private val bullet = """^(?:[-*•·–—]|\[[ xX✓✔]?]|[☐☑✓✔]|\d+[.)])\s+""".toRegex()
    private val trailingNote = """^(.*?)\s*\(([^()]*)\)$""".toRegex()

    /**
     * What's still to buy on the list, as in "- 2 l milk", under a heading per aisle. The items
     * without a category are headed by [noCategory], unless they're all there is.
     */
    fun format(overview: ShoppingListOverview, noCategory: String): String = buildString {
        append(overview.list.name)
        val headed = overview.aisles.any { it.category != null }
        overview.aisles.forEach { aisle ->
            if (headed) append("\n\n").append(aisle.category?.let { "${it.emoji} ${it.name}" } ?: noCategory)
            aisle.items.forEach { append("\n- ").append(it.asLine()) }
        }
    }

    /** An item as it's typed into the add field, so [parse] reads it back the same. */
    private fun ShoppingItem.asLine(): String = buildString {
        if (quantity != 1.0 || unit != QuantityUnit.PIECES) {
            append(BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString())
            unit.word?.let { append(' ').append(it) }
            append(' ')
        }
        append(name)
        note?.let { append(" (").append(it).append(')') }
    }

    private val QuantityUnit.word: String?
        get() = when (this) {
            QuantityUnit.PIECES -> null
            QuantityUnit.PACKS -> "packs"
            QuantityUnit.GRAMS -> "g"
            QuantityUnit.KILOGRAMS -> "kg"
            QuantityUnit.MILLILITERS -> "ml"
            QuantityUnit.LITERS -> "l"
        }

    /**
     * Reads the items in text, one per line, as typed into the add field ("2 milk", "eggs x12"),
     * with a note in brackets at the end. List markers are dropped. When some lines have them, the
     * lines without are headings rather than items, and a heading naming one of [categories] puts
     * the items under it in that category.
     */
    fun parse(text: String, categories: List<Category>): List<ShoppingEntry> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val hasBullets = lines.any { bullet.containsMatchIn(it) }
        val categoriesByName = categories.associateBy { productKey(it.name) }
        var category: Category? = null
        return lines.mapNotNull { line ->
            val marker = bullet.find(line)
            if (hasBullets && marker == null) {
                // A heading, such as the list's name or a category, with any emoji in front.
                category = categoriesByName[productKey(line.dropWhile { !it.isLetterOrDigit() })]
                return@mapNotNull null
            }
            val content = marker?.let { line.substring(it.range.last + 1) } ?: line
            val note = trailingNote.matchEntire(content)
            val entry = ShoppingEntry.parse(note?.groupValues?.get(1) ?: content) ?: return@mapNotNull null
            entry.copy(category = category, note = note?.groupValues?.get(2)?.trim()?.ifEmpty { null })
        }
    }
}
