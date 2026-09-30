package io.github.abhik9.expirywatch.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.data.repository.ContentResolverDocumentStore
import io.github.abhik9.expirywatch.core.data.repository.DataStoreUserSettingsRepository
import io.github.abhik9.expirywatch.core.data.repository.OpenFoodFactsProductCatalog
import io.github.abhik9.expirywatch.core.data.repository.RoomBackupRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomCategoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomItemRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomProductHistoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomStorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.ProductCatalog
import io.github.abhik9.expirywatch.core.domain.repository.ProductHistoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsItemRepository(repository: RoomItemRepository): ItemRepository

    @Binds
    abstract fun bindsCategoryRepository(repository: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindsStorageLocationRepository(repository: RoomStorageLocationRepository): StorageLocationRepository

    @Binds
    abstract fun bindsProductHistoryRepository(repository: RoomProductHistoryRepository): ProductHistoryRepository

    @Binds
    abstract fun bindsProductCatalog(catalog: OpenFoodFactsProductCatalog): ProductCatalog

    @Binds
    abstract fun bindsBackupRepository(repository: RoomBackupRepository): BackupRepository

    @Binds
    abstract fun bindsDocumentStore(store: ContentResolverDocumentStore): DocumentStore

    @Binds
    abstract fun bindsUserSettingsRepository(repository: DataStoreUserSettingsRepository): UserSettingsRepository
}
