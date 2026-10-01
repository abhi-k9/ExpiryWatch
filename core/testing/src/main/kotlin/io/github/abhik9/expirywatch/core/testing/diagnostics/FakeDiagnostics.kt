package io.github.abhik9.expirywatch.core.testing.diagnostics

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.DiagnosticsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps every recorded message, to check what code under test records. */
class RecordingEventLog : EventLog {
    val messages = mutableListOf<String>()

    override fun record(message: () -> String) {
        messages += message()
    }
}

class FakeDiagnosticsRepository(var report: String = "diagnostics report") : DiagnosticsRepository {
    override val isRecording = MutableStateFlow(false)
    override val logSize = MutableStateFlow(0L)

    override fun setRecording(enabled: Boolean) {
        isRecording.value = enabled
    }

    override suspend fun clear() {
        logSize.value = 0
    }

    override suspend fun report(): String = report
}
