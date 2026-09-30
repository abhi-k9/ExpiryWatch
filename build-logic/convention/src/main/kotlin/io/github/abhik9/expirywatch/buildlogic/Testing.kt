package io.github.abhik9.expirywatch.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.withType

/** Shared settings for JVM and Android (local) unit test tasks. */
internal fun Project.configureTests() {
    tasks.withType<Test>().configureEach {
        // Robolectric's native SQLite reaches into JDK internals to manage file descriptors.
        jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        // Modules without tests still get generated test classes (e.g. from Hilt).
        failOnNoDiscoveredTests.set(false)
        testLogging {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
