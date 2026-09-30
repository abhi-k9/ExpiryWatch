package io.github.abhik9.expirywatch.core.domain.insights

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.MonthlyUsage
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsCalculatorTest {
    private val today = TestTime.today // 2026-03-10

    private fun finished(
        status: ItemStatus,
        finishedOn: LocalDate,
        expiry: LocalDate = finishedOn,
        category: Category? = TestData.dairy,
    ): Item = TestData.item(category = category).copy(
        status = status,
        finishedDate = finishedOn,
        expiryDate = expiry,
    )

    @Test
    fun countsOnlyItemsFinishedWithinThePeriod() {
        val items = listOf(
            finished(ItemStatus.CONSUMED, finishedOn = today),
            finished(ItemStatus.CONSUMED, finishedOn = LocalDate.of(2026, 3, 1)),
            finished(ItemStatus.WASTED, finishedOn = LocalDate.of(2026, 3, 2)),
            // February: outside a one-month period, inside three months.
            finished(ItemStatus.WASTED, finishedOn = LocalDate.of(2026, 2, 27)),
            // Still active: never counted.
            TestData.item(),
        )

        val oneMonth = InsightsCalculator.calculate(items, InsightsPeriod.ONE_MONTH, today)
        assertEquals(2, oneMonth.consumedCount)
        assertEquals(1, oneMonth.wastedCount)

        val threeMonths = InsightsCalculator.calculate(items, InsightsPeriod.THREE_MONTHS, today)
        assertEquals(2, threeMonths.consumedCount)
        assertEquals(2, threeMonths.wastedCount)
        assertEquals(0.5, threeMonths.wasteRate)
    }

    @Test
    fun countsItemsConsumedByTheirExpiryDate() {
        val items = listOf(
            finished(ItemStatus.CONSUMED, finishedOn = today, expiry = today),
            finished(ItemStatus.CONSUMED, finishedOn = today, expiry = today.minusDays(1)),
        )

        assertEquals(1, InsightsCalculator.calculate(items, InsightsPeriod.ONE_MONTH, today).consumedInTimeCount)
    }

    @Test
    fun monthlyTrendCoversAtLeastSixMonthsOldestFirst() {
        val items = listOf(
            finished(ItemStatus.CONSUMED, finishedOn = today),
            finished(ItemStatus.WASTED, finishedOn = LocalDate.of(2025, 10, 15)),
        )

        val monthly = InsightsCalculator.calculate(items, InsightsPeriod.ONE_MONTH, today).monthly

        assertEquals(6, monthly.size)
        assertEquals(YearMonth.of(2025, 10), monthly.first().month)
        assertEquals(MonthlyUsage(YearMonth.of(2025, 10), consumed = 0, wasted = 1), monthly.first())
        assertEquals(MonthlyUsage(YearMonth.of(2026, 3), consumed = 1, wasted = 0), monthly.last())
        assertEquals(12, InsightsCalculator.calculate(items, InsightsPeriod.TWELVE_MONTHS, today).monthly.size)
    }

    @Test
    fun ranksCategoriesByWastedItems() {
        val items = listOf(
            finished(ItemStatus.WASTED, today, category = TestData.produce),
            finished(ItemStatus.WASTED, today, category = TestData.produce),
            finished(ItemStatus.WASTED, today, category = TestData.dairy),
            finished(ItemStatus.WASTED, today, category = null),
            finished(ItemStatus.CONSUMED, today, category = TestData.dairy),
        )

        val top = InsightsCalculator.calculate(items, InsightsPeriod.ONE_MONTH, today).topWastedCategories

        assertEquals(listOf(TestData.produce, TestData.dairy, null), top.map { it.category })
        assertEquals(listOf(2, 1, 1), top.map { it.wastedCount })
    }

    @Test
    fun earliestRelevantDateCoversTheLongestPeriod() {
        assertEquals(LocalDate.of(2025, 4, 1), InsightsCalculator.earliestRelevantDate(today))
    }
}
