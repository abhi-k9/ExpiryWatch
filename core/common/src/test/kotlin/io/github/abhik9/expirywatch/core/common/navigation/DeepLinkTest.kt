package io.github.abhik9.expirywatch.core.common.navigation

import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkTest {
    @Test
    fun everyDeepLinkRoundTripsThroughItsUri() {
        val links = listOf(
            DeepLink.Items(),
            DeepLink.Items(ExpiryStatus.EXPIRED),
            DeepLink.Items(ExpiryStatus.EXPIRING_SOON),
            DeepLink.Items(ExpiryStatus.FRESH),
            DeepLink.EditItem(42),
            DeepLink.AddItem(),
            DeepLink.AddItem(scanBarcode = true),
        )

        links.forEach { link ->
            assertEquals(link, DeepLink.parse(link.toUri()), "Round trip of ${link.toUri()}")
        }
    }

    @Test
    fun urisAreStable() {
        assertEquals("expirywatch://items?status=expiring_soon", DeepLink.Items(ExpiryStatus.EXPIRING_SOON).toUri())
        assertEquals("expirywatch://items/7", DeepLink.EditItem(7).toUri())
        assertEquals("expirywatch://add?scan=true", DeepLink.AddItem(scanBarcode = true).toUri())
    }

    @Test
    fun unknownOrMalformedUrisAreRejected() {
        assertNull(DeepLink.parse("https://items"))
        assertNull(DeepLink.parse("expirywatch://unknown"))
        assertNull(DeepLink.parse("expirywatch://items/not-a-number"))
        assertNull(DeepLink.parse("expirywatch://items/1/2"))
        assertNull(DeepLink.parse("not a uri"))
    }

    @Test
    fun unknownStatusFallsBackToUnfilteredList() {
        assertEquals(DeepLink.Items(status = null), DeepLink.parse("expirywatch://items?status=bogus"))
    }
}
