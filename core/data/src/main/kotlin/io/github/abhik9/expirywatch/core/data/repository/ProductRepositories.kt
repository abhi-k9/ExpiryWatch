package io.github.abhik9.expirywatch.core.data.repository

import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.repository.ProductCatalog
import io.github.abhik9.expirywatch.core.domain.repository.ProductHistoryRepository
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.network.OpenFoodFactsDataSource
import java.util.Locale
import javax.inject.Inject

internal class RoomProductHistoryRepository @Inject constructor(
    private val productDao: ProductDao,
) : ProductHistoryRepository {
    override suspend fun find(barcode: String): ProductInfo? = productDao.find(barcode)?.asExternalModel()

    override suspend fun remember(product: ProductInfo) = productDao.upsert(product.asEntity())
}

internal class OpenFoodFactsProductCatalog @Inject constructor(
    private val dataSource: OpenFoodFactsDataSource,
) : ProductCatalog {
    override suspend fun find(barcode: String): ProductInfo? =
        dataSource.getProduct(barcode, languageCode = Locale.getDefault().language)?.let { product ->
            ProductInfo(
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                imageUrl = product.imageUrl,
                source = ProductSource.OPEN_FOOD_FACTS,
            )
        }
}
