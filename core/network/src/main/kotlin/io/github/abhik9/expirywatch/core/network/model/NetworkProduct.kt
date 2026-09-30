package io.github.abhik9.expirywatch.core.network.model

/** A product as described by the Open Food Facts database. */
data class NetworkProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
)
