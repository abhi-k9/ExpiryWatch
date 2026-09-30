package io.github.abhik9.expirywatch.core.network

import io.github.abhik9.expirywatch.core.network.di.createOpenFoodFactsApi
import io.github.abhik9.expirywatch.core.network.model.NetworkProduct
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

class OpenFoodFactsDataSourceTest {
    private val server = MockWebServer()
    private lateinit var dataSource: OpenFoodFactsDataSource

    @BeforeTest
    fun setUp() {
        server.start()
        val api = createOpenFoodFactsApi(
            baseUrl = server.url("/").toString(),
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        )
        dataSource = OpenFoodFactsDataSource(api)
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun parsesAFoundProductPreferringTheLocalizedName() = runTest {
        respond(
            200,
            """
            {
              "code": "3017620422003",
              "status": 1,
              "status_verbose": "product found",
              "product": {
                "product_name": "Nutella",
                "product_name_de": "Nutella Nuss-Nougat-Creme",
                "brands": "Ferrero, Nutella",
                "image_front_url": "https://images.openfoodfacts.org/front.jpg",
                "product_quantity": 400
              }
            }
            """.trimIndent(),
        )

        val product = dataSource.getProduct("3017620422003", languageCode = "de")

        assertEquals(
            NetworkProduct(
                barcode = "3017620422003",
                name = "Nutella Nuss-Nougat-Creme",
                brand = "Ferrero",
                imageUrl = "https://images.openfoodfacts.org/front.jpg",
            ),
            product,
        )
        val request = server.takeRequest()
        assertTrue(request.url.encodedPath.endsWith("/api/v2/product/3017620422003"))
        assertTrue(request.url.queryParameter("fields")!!.contains("product_name_de"))
    }

    @Test
    fun fallsBackToTheDefaultName() = runTest {
        respond(200, """{"code": "1", "status": 1, "product": {"product_name": "Milk", "brands": ""}}""")

        val product = dataSource.getProduct("1", languageCode = "fr")

        assertEquals(NetworkProduct(barcode = "1", name = "Milk", brand = null, imageUrl = null), product)
    }

    @Test
    fun returnsNullForUnknownProducts() = runTest {
        respond(404, """{"code": "0000000000000", "status": 0, "status_verbose": "product not found"}""")
        assertNull(dataSource.getProduct("0000000000000", "en"))

        respond(200, """{"code": "0000000000000", "status": 0}""")
        assertNull(dataSource.getProduct("0000000000000", "en"))
    }

    @Test
    fun returnsNullForProductsWithoutAName() = runTest {
        respond(200, """{"code": "1", "status": 1, "product": {"brands": "Acme"}}""")
        assertNull(dataSource.getProduct("1", "en"))
    }

    @Test
    fun throwsIoExceptionForServerErrorsAndGarbage() = runTest {
        respond(503, "Service unavailable")
        assertFailsWith<IOException> { dataSource.getProduct("1", "en") }

        respond(200, "<html>not json</html>")
        assertFailsWith<IOException> { dataSource.getProduct("1", "en") }
    }
}
