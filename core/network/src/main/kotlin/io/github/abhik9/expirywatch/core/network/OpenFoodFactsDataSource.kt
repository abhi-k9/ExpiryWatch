package io.github.abhik9.expirywatch.core.network

import io.github.abhik9.expirywatch.core.network.model.NetworkProduct
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Looks products up by barcode in the Open Food Facts database. */
@Singleton
class OpenFoodFactsDataSource @Inject internal constructor(
    private val api: OpenFoodFactsApi,
) {
    /**
     * @param languageCode an ISO 639-1 code; the product name in that language is preferred.
     * @return the product, or `null` when Open Food Facts doesn't know the barcode.
     * @throws IOException when the service can't be reached or answers with an error.
     */
    suspend fun getProduct(barcode: String, languageCode: String): NetworkProduct? {
        val localizedName = "product_name_${languageCode.lowercase()}"
        val fields = listOf(localizedName, *BASE_FIELDS).joinToString(",")

        val response = try {
            api.getProduct(barcode, fields)
        } catch (e: SerializationException) {
            throw IOException("Unexpected response from Open Food Facts", e)
        }

        if (response.code() == HTTP_NOT_FOUND) return null
        if (!response.isSuccessful) throw IOException("Open Food Facts answered HTTP ${response.code()}")

        val body = response.body() ?: return null
        val product = body.product?.takeIf { body.status == STATUS_FOUND } ?: return null

        val name = product.string(localizedName)
            ?: product.string("product_name")
            ?: product.string("generic_name")
            ?: return null

        return NetworkProduct(
            barcode = body.code ?: barcode,
            name = name,
            brand = product.string("brands")?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = product.string("image_front_url")
                ?: product.string("image_url")
                ?: product.string("image_front_small_url"),
        )
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val STATUS_FOUND = 1
        val BASE_FIELDS = arrayOf(
            "code",
            "product_name",
            "generic_name",
            "brands",
            "image_front_url",
            "image_url",
            "image_front_small_url",
        )
    }
}
