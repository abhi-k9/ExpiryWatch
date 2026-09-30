package io.github.abhik9.expirywatch.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsTest {
    private fun insights(consumed: Int, wasted: Int) = Insights(
        period = InsightsPeriod.ONE_MONTH,
        consumedCount = consumed,
        wastedCount = wasted,
        consumedInTimeCount = consumed,
        monthly = emptyList(),
        topWastedCategories = emptyList(),
    )

    @Test
    fun wasteRateIsNullWithoutFinishedItems() {
        assertNull(insights(consumed = 0, wasted = 0).wasteRate)
    }

    @Test
    fun wasteRateIsShareOfFinishedItems() {
        assertEquals(0.25, insights(consumed = 3, wasted = 1).wasteRate)
    }
}
