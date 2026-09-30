package io.github.abhik9.expirywatch.core.domain.insights

import io.github.abhik9.expirywatch.core.model.CategoryWaste
import io.github.abhik9.expirywatch.core.model.Insights
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.MonthlyUsage
import io.github.abhik9.expirywatch.core.model.effectiveExpiryDate
import java.time.LocalDate
import java.time.YearMonth

object InsightsCalculator {
    /** The monthly trend always covers at least this many months, so it reads as a trend. */
    const val MIN_TREND_MONTHS = 6
    const val TOP_CATEGORY_COUNT = 5

    /** The earliest finish date any period can need, for loading just enough history. */
    fun earliestRelevantDate(today: LocalDate): LocalDate {
        val maxMonths = maxOf(MIN_TREND_MONTHS, InsightsPeriod.entries.maxOf { it.months })
        return YearMonth.from(today).minusMonths(maxMonths - 1L).atDay(1)
    }

    /**
     * @param finishedItems items that were consumed or wasted; active items are ignored.
     * @param today the last day of the period.
     */
    fun calculate(finishedItems: List<Item>, period: InsightsPeriod, today: LocalDate): Insights {
        val finished = finishedItems.mapNotNull { item ->
            val finishedDate = item.finishedDate
            if (item.status == ItemStatus.ACTIVE || finishedDate == null) null else FinishedItem(item, finishedDate)
        }

        // A period of N months covers the last N calendar months, including the current one.
        val periodStart = YearMonth.from(today).minusMonths(period.months - 1L).atDay(1)
        val inPeriod = finished.filter { it.finishedDate in periodStart..today }
        val consumed = inPeriod.filter { it.item.status == ItemStatus.CONSUMED }
        val wasted = inPeriod.filter { it.item.status == ItemStatus.WASTED }

        return Insights(
            period = period,
            consumedCount = consumed.size,
            wastedCount = wasted.size,
            consumedInTimeCount = consumed.count { it.finishedDate <= it.item.effectiveExpiryDate },
            monthly = monthlyUsage(finished, today, trendMonths = maxOf(MIN_TREND_MONTHS, period.months)),
            topWastedCategories = wasted
                .groupBy { it.item.category?.id }
                .map { (_, items) -> CategoryWaste(category = items.first().item.category, wastedCount = items.size) }
                .sortedWith(
                    compareByDescending<CategoryWaste> { it.wastedCount }.thenBy(nullsLast()) { it.category?.name },
                )
                .take(TOP_CATEGORY_COUNT),
        )
    }

    private fun monthlyUsage(finished: List<FinishedItem>, today: LocalDate, trendMonths: Int): List<MonthlyUsage> {
        val currentMonth = YearMonth.from(today)
        val byMonth = finished.groupBy { YearMonth.from(it.finishedDate) }
        return (trendMonths - 1 downTo 0).map { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo.toLong())
            val items = byMonth[month].orEmpty()
            MonthlyUsage(
                month = month,
                consumed = items.count { it.item.status == ItemStatus.CONSUMED },
                wasted = items.count { it.item.status == ItemStatus.WASTED },
            )
        }
    }

    private data class FinishedItem(val item: Item, val finishedDate: LocalDate)
}
