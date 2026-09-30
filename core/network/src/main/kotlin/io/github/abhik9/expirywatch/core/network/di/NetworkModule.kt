package io.github.abhik9.expirywatch.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.network.OpenFoodFactsApi
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The User-Agent sent with every request. Open Food Facts asks apps to identify themselves with
 * their name, version and a contact URL. Provided by the app module, which knows its version.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UserAgent

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/"
    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Shared by API calls and image loading, so they share a connection pool. */
    @Provides
    @Singleton
    fun providesOkHttpClient(@UserAgent userAgent: String): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }
        .build()

    @Provides
    @Singleton
    fun providesOpenFoodFactsApi(okHttpClient: OkHttpClient, json: Json): OpenFoodFactsApi =
        createOpenFoodFactsApi(OPEN_FOOD_FACTS_BASE_URL, okHttpClient, json)
}

internal fun createOpenFoodFactsApi(baseUrl: String, okHttpClient: OkHttpClient, json: Json): OpenFoodFactsApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory { okHttpClient.newCall(it) }
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenFoodFactsApi::class.java)
