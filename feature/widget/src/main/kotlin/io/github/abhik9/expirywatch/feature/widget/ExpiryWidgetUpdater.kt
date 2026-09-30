package io.github.abhik9.expirywatch.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Redraws every placed ExpiryWatch widget, e.g. after items change. */
class ExpiryWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun updateAll() {
        ExpiryWidget().updateAll(context)
    }
}
