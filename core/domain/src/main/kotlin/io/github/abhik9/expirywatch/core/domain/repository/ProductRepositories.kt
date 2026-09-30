package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.ProductInfo

/** Products the user has saved before, keyed by barcode. */
interface ProductHistoryRepository {
    suspend fun find(barcode: String): ProductInfo?

    suspend fun remember(product: ProductInfo)
}

/** An online product database, such as Open Food Facts. */
interface ProductCatalog {
    /**
     * Returns the product, or `null` when the catalog doesn't know the barcode.
     *
     * @throws java.io.IOException when the catalog can't be reached.
     */
    suspend fun find(barcode: String): ProductInfo?
}
