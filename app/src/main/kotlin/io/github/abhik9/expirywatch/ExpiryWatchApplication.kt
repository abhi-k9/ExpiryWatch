package io.github.abhik9.expirywatch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.usecase.SyncReminderScheduleUseCase
import io.github.abhik9.expirywatch.feature.widget.ExpiryWidgetUpdater
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@HiltAndroidApp
class ExpiryWatchApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var syncReminderSchedule: SyncReminderScheduleUseCase

    @Inject lateinit var itemRepository: ItemRepository

    @Inject lateinit var widgetUpdater: ExpiryWidgetUpdater

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        AppShortcuts.publish(this)

        // Keep the daily reminder in line with the settings for as long as the process lives.
        applicationScope.launch { syncReminderSchedule() }

        // Redraw home-screen widgets whenever items change; debounced to batch quick edits.
        applicationScope.launch {
            itemRepository.observeActiveItems()
                .debounce(WIDGET_UPDATE_DEBOUNCE)
                .collect { widgetUpdater.updateAll() }
        }
    }

    /** Product images load through the app's OkHttp client, sharing its connection pool. */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
        .crossfade(true)
        .build()

    private companion object {
        val WIDGET_UPDATE_DEBOUNCE = 1.seconds
    }
}
