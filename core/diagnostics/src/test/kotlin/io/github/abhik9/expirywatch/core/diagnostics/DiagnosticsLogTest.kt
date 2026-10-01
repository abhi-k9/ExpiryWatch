package io.github.abhik9.expirywatch.core.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.testing.TestTime
import java.util.concurrent.Executor
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiagnosticsLogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Writes on the calling thread, so tests see each line as soon as it's recorded.
    private fun newLog(maxFileBytes: Long = 64 * 1024) =
        DiagnosticsLog(context, TestTime.clock, Executor(Runnable::run), maxFileBytes)

    private suspend fun recordingLog(maxFileBytes: Long = 64 * 1024) = newLog(maxFileBytes).apply {
        setRecording(true)
        clear()
    }

    @Test
    fun nothingIsBuiltOrWrittenWhileRecordingIsOff() = runTest {
        val log = newLog()
        log.setRecording(false)
        log.clear()

        var built = false
        log.record {
            built = true
            "never"
        }

        assertFalse(built)
        assertEquals("", log.read())
        assertEquals(0, log.size.value)
    }

    @Test
    fun linesAreRecordedInOrderWithTimeAndThread() = runTest {
        val log = recordingLog()

        log.record { "first" }
        log.record { "second" }

        val thread = Thread.currentThread().name
        val expected = "2026-03-10T12:00Z [$thread] first\n2026-03-10T12:00Z [$thread] second\n"
        assertEquals(expected, log.read())
        assertEquals(expected.toByteArray().size.toLong(), log.size.value)
    }

    @Test
    fun recordingIsRememberedAndBothEndsAreMarked() = runTest {
        val log = recordingLog()
        log.record { "while recording" }
        log.setRecording(false)
        log.record { "after" }

        val lines = log.read().lines()
        assertTrue(lines[0].endsWith("while recording"))
        assertTrue(lines[1].endsWith("diagnostics: recording stopped"))
        assertFalse(newLog().isRecording.value)

        log.setRecording(true)
        assertTrue(log.read().lines()[2].endsWith("diagnostics: recording started"))
        assertTrue(newLog().isRecording.value)
    }

    @Test
    fun oldLinesAreRotatedOutToBoundTheSize() = runTest {
        val log = recordingLog(maxFileBytes = 200)

        repeat(20) { i -> log.record { "line $i".padEnd(40, '.') } }

        val text = log.read()
        assertFalse("line 0." in text)
        assertTrue("line 19" in text)
        // The current file plus one previous file, each at most one line over the limit.
        assertTrue(log.size.value < 2 * (200 + 80), "size ${log.size.value}")
        assertEquals(text.toByteArray().size.toLong(), log.size.value)
    }

    @Test
    fun aFailingMessageIsRecordedInsteadOfThrown() = runTest {
        val log = recordingLog()

        log.record { error("boom") }

        assertTrue(log.read().trim().endsWith("<message failed: java.lang.IllegalStateException: boom>"))
    }

    @Test
    fun clearingEmptiesTheLog() = runTest {
        val log = recordingLog()
        log.record { "something" }

        log.clear()

        assertEquals("", log.read())
        assertEquals(0, log.size.value)
    }

    @Test
    fun reportListsSectionsByTitleThenTheLog() = runTest {
        val log = recordingLog()
        log.record { "an event" }
        val sections = setOf(
            section("Device") { listOf("Android: 16") },
            section("App") { listOf("Version: 1.0.0") },
            section("Broken") { error("boom") },
        )
        val repository = DiagnosticsReportRepository(log, Provider { sections }, TestTime.clock)

        val report = repository.report()

        val expected = """
            ExpiryWatch diagnostics
            Generated: 2026-03-10T12:00Z

            == App ==
            Version: 1.0.0

            == Broken ==
            Unavailable: java.lang.IllegalStateException: boom

            == Device ==
            Android: 16

            == Log ==

        """.trimIndent()
        assertTrue(report.startsWith(expected), report)
        assertTrue(report.trim().endsWith("an event"), report)
    }

    private fun section(name: String, lines: () -> List<String>) = object : DiagnosticsSection {
        override val title = name

        override suspend fun describe() = lines()
    }
}
