package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.testing.diagnostics.FakeDiagnosticsRepository
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.expirywatch.core.testing.repository.FakeDocumentStore
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ExportDiagnosticsUseCaseTest {
    private val documents = FakeDocumentStore()
    private val log = RecordingEventLog()
    private val export = ExportDiagnosticsUseCase(FakeDiagnosticsRepository(report = "the report"), documents, log)

    @Test
    fun writesTheReport() = runTest {
        assertTrue(export("content://diagnostics"))
        assertEquals("the report", documents.documents["content://diagnostics"])
    }

    @Test
    fun recordsFailures() = runTest {
        documents.failWith = IOException("disk full")

        assertFalse(export("content://diagnostics"))
        assertEquals(listOf("diagnostics: export failed: java.io.IOException: disk full"), log.messages)
    }
}
