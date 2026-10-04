package io.github.abhik9.expirywatch.core.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.data.repository.ContentResolverDocumentStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ContentResolverDocumentStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resolver = shadowOf(context.contentResolver)
    private val store = ContentResolverDocumentStore(context, Dispatchers.Unconfined)
    private val uri = Uri.parse("content://io.github.abhik9.expirywatch.test/backup.json")

    @Test
    fun writesAndReadsDocuments() = runTest {
        val written = ByteArrayOutputStream()
        resolver.registerOutputStream(uri, written)
        resolver.registerInputStream(uri, ByteArrayInputStream("{\"read\": true}".toByteArray()))

        store.writeText(uri.toString(), "{\"written\": true}")

        assertEquals("{\"written\": true}", written.toString(Charsets.UTF_8.name()))
        assertEquals("{\"read\": true}", store.readText(uri.toString()))
    }

    @Test
    fun providerFailuresAreReportedAsIoErrors() = runTest {
        // As a provider does for a document the user no longer gives the app access to.
        resolver.registerOutputStreamSupplier(uri) { throw SecurityException("Permission denied") }
        resolver.registerInputStreamSupplier(uri) { throw SecurityException("Permission denied") }

        assertFailsWith<IOException> { store.writeText(uri.toString(), "{}") }
        assertFailsWith<IOException> { store.readText(uri.toString()) }
    }
}
