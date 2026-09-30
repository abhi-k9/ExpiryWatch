package io.github.abhik9.expirywatch.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.BuildConfig
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.network.di.UserAgent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesAppInfo(): AppInfo = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        barcodeEngine = BuildConfig.BARCODE_ENGINE,
        sourceCodeUrl = BuildConfig.SOURCE_CODE_URL,
    )

    @Provides
    @UserAgent
    fun providesUserAgent(appInfo: AppInfo): String =
        "ExpiryWatch/${appInfo.versionName} (Android; +${appInfo.sourceCodeUrl})"
}
