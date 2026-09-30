package io.github.abhik9.expirywatch.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The subset of the Open Food Facts API v2 used by the app.
 * https://openfoodfacts.github.io/openfoodfacts-server/api/
 */
internal interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String,
    ): Response<OffProductResponse>
}

@Serializable
internal data class OffProductResponse(
    val code: String? = null,
    /** 1 when the product was found, 0 otherwise. */
    val status: Int = 0,
    /** Kept as a raw object: localized names live under language-specific keys. */
    val product: JsonObject? = null,
)
