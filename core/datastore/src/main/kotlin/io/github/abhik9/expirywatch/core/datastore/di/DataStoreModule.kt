package io.github.abhik9.expirywatch.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.common.di.AppDispatcher
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.common.di.Dispatcher
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun providesUserSettingsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // A corrupt settings file shouldn't crash the app: fall back to the defaults.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope + ioDispatcher,
        produceFile = { context.preferencesDataStoreFile("user_settings") },
    )
}
