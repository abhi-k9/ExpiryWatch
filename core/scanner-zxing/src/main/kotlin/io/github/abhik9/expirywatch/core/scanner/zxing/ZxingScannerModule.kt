package io.github.abhik9.expirywatch.core.scanner.zxing

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.scanner.BarcodeAnalyzerFactory

@Module
@InstallIn(SingletonComponent::class)
internal object ZxingScannerModule {
    @Provides
    fun providesBarcodeAnalyzerFactory(): BarcodeAnalyzerFactory = BarcodeAnalyzerFactory(::ZxingBarcodeAnalyzer)
}
