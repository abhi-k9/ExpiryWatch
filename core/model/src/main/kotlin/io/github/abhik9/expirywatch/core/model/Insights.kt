package io.github.abhik9.expirywatch.core.model

import java.time.YearMonth

/** Usage and waste statistics for items finished within [period]. */
data class Insights(
    val period: InsightsPeriod,
    val consumedCount: Int,
    val wastedCount: Int,
    /** Items that were used up on or before their expiry date. */
    val consumedInTimeCount: Int,
    /** Monthly totals, oldest first. */
    val monthly: List<MonthlyUsage>,
    /** Categories with the most wasted items, most wasted first. */
    val topWastedCategories: List<CategoryWaste>,
) {
    val finishedCount: Int get() = consumedCount + wastedCount

    /** Share of finished items that were wasted, or `null` when nothing was finished. */
    val wasteRate: Double?
        get() = if (finishedCount == 0) null else wastedCount.toDouble() / finishedCount
}

enum class InsightsPeriod(val months: Int) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    TWELVE_MONTHS(12),
}

data class MonthlyUsage(
    val month: YearMonth,
    val consumed: Int,
    val wasted: Int,
)

data class CategoryWaste(
    /** `null` for items without a category. */
    val category: Category?,
    val wastedCount: Int,
)
