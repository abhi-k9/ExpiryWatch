package io.github.abhik9.expirywatch.core.diagnostics

import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.domain.repository.DiagnosticsRepository
import java.time.Clock
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.StateFlow

/** Combines the sections contributed by the app's modules with the recorded [DiagnosticsLog]. */
internal class DiagnosticsReportRepository @Inject constructor(
    private val log: DiagnosticsLog,
    // A provider, so that sections are only created when a report is built.
    private val sections: Provider<Set<@JvmSuppressWildcards DiagnosticsSection>>,
    private val clock: Clock,
) : DiagnosticsRepository {
    override val isRecording: StateFlow<Boolean> get() = log.isRecording

    override val logSize: StateFlow<Long> get() = log.size

    override fun setRecording(enabled: Boolean) = log.setRecording(enabled)

    override suspend fun clear() = log.clear()

    override suspend fun report(): String = buildString {
        appendLine("ExpiryWatch diagnostics")
        appendLine("Generated: ${OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)}")
        for (section in sections.get().sortedBy { it.title }) {
            appendLine()
            appendLine("== ${section.title} ==")
            section.describeSafely().forEach(::appendLine)
        }
        appendLine()
        appendLine("== Log ==")
        append(log.read())
    }

    /** One broken section shouldn't prevent exporting the rest. */
    private suspend fun DiagnosticsSection.describeSafely(): List<String> = try {
        describe()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        listOf("Unavailable: $e")
    }
}
