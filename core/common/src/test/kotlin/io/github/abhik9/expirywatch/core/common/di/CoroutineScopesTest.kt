package io.github.abhik9.expirywatch.core.common.di

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CoroutineScopesTest {
    @Test
    fun failingBackgroundWorkIsRecordedAndTheScopeCarriesOn() = runBlocking {
        val messages = mutableListOf<String>()
        val log = EventLog { messages += it() }
        val scope = CoroutineScopesModule.providesApplicationScope(Dispatchers.Unconfined, log)

        scope.launch { error("widget update failed") }.join()
        var ranAfterwards = false
        scope.launch { ranAfterwards = true }.join()

        val expected = "background work failed: java.lang.IllegalStateException: widget update failed"
        assertTrue(messages.single().startsWith(expected), messages.single())
        assertEquals(true, ranAfterwards)
    }
}
