package io.github.abhik9.expirywatch.core.domain

import io.github.abhik9.expirywatch.core.domain.usecase.normalizedForSearch

private val whitespace = "\\s+".toRegex()

/**
 * What identifies a product by its name: the name ignoring case, accents and spacing, so that
 * "Crème fraîche" and " creme  FRAICHE" are the same product.
 */
fun productKey(name: String): String = name.normalizedForSearch().replace(whitespace, " ")
