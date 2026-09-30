package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.repository.ProductCatalog
import io.github.abhik9.expirywatch.core.domain.repository.ProductHistoryRepository
import io.github.abhik9.expirywatch.core.model.ProductInfo
import java.io.IOException

class FakeProductHistoryRepository : ProductHistoryRepository {
    val products = mutableMapOf<String, ProductInfo>()

    override suspend fun find(barcode: String): ProductInfo? = products[barcode]

    override suspend fun remember(product: ProductInfo) {
        products[product.barcode] = product
    }
}

class FakeProductCatalog : ProductCatalog {
    val products = mutableMapOf<String, ProductInfo>()
    var isOffline = false
    val requestedBarcodes = mutableListOf<String>()

    override suspend fun find(barcode: String): ProductInfo? {
        requestedBarcodes += barcode
        if (isOffline) throw IOException("Offline")
        return products[barcode]
    }
}
