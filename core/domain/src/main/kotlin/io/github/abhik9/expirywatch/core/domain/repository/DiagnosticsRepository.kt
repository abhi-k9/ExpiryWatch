package io.github.abhik9.expirywatch.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The diagnostics log: what the app did, recorded on the device only while the user has turned
 * recording on, and only shared when they export it.
 */
interface DiagnosticsRepository {
    val isRecording: StateFlow<Boolean>

    /** Size of the recorded log, in bytes. */
    val logSize: StateFlow<Long>

    fun setRecording(enabled: Boolean)

    suspend fun clear()

    /** The state of the app and device, followed by the recorded log, oldest entries first. */
    suspend fun report(): String
}
