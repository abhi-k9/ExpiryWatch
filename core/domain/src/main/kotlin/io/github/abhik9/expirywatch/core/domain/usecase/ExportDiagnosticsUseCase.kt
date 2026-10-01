package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.DiagnosticsRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import java.io.IOException
import javax.inject.Inject

/** Writes the diagnostics report to the document at [uri][invoke]. Returns whether it worked. */
class ExportDiagnosticsUseCase @Inject constructor(
    private val diagnostics: DiagnosticsRepository,
    private val documentStore: DocumentStore,
    private val log: EventLog,
) {
    suspend operator fun invoke(uri: String): Boolean = try {
        documentStore.writeText(uri, diagnostics.report())
        true
    } catch (e: IOException) {
        log.record { "diagnostics: export failed: $e" }
        false
    }
}
