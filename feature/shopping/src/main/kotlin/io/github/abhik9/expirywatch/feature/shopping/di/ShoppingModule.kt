package io.github.abhik9.expirywatch.feature.shopping.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.domain.shopping.DefaultShoppingListName
import io.github.abhik9.expirywatch.feature.shopping.R

@Module
@InstallIn(SingletonComponent::class)
internal object ShoppingModule {
    // Not cached, so it follows the user's language.
    @Provides
    @DefaultShoppingListName
    fun providesDefaultShoppingListName(@ApplicationContext context: Context): String =
        context.getString(R.string.feature_shopping_default_list_name)
}
