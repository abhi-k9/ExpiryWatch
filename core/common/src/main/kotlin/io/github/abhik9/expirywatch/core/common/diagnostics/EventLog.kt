package io.github.abhik9.expirywatch.core.common.diagnostics

/**
 * Records what the app does, to help investigate issues the user reports. Only for technical
 * details such as ids, counts and errors: never personal data such as item names or notes.
 *
 * Messages are lazy, so they aren't even built while recording is off.
 */
fun interface EventLog {
    fun record(message: () -> String)

    companion object {
        /** Records nothing. */
        val NONE: EventLog = EventLog { }
    }
}

/**
 * Describes the current state of one part of the app, at the top of an exported diagnostics log.
 * Like [EventLog], only technical details.
 */
interface DiagnosticsSection {
    /** Sections are sorted by title. */
    val title: String

    /** Lines such as "Notifications enabled: true". */
    suspend fun describe(): List<String>
}
