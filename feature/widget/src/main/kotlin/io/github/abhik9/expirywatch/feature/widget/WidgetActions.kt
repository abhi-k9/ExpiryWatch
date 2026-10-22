package io.github.abhik9.expirywatch.feature.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.EntryPointAccessors
import io.github.abhik9.expirywatch.core.model.ItemStatus

/** Where each widget keeps its [WidgetFilter]. */
internal val FilterKey = stringPreferencesKey("filter")

/** Shows the widget's next filter, given in [NextFilter] as it's [encoded][WidgetFilter.encode]. */
internal class ChangeFilterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val next = parameters[NextFilter] ?: return
        updateAppWidgetState(context, glanceId) { it[FilterKey] = next }
        ExpiryWidget().update(context, glanceId)
    }

    companion object {
        val NextFilter = ActionParameters.Key<String>("next_filter")
    }
}

/** Marks the item with [ItemId] used up or thrown away, as [Outcome] says; the widget follows the change. */
internal class FinishItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ItemId] ?: return
        val outcome = parameters[Outcome]?.let { name -> ItemStatus.entries.firstOrNull { it.name == name } } ?: return
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .finishItemUseCase()(itemId, outcome)
    }

    companion object {
        val ItemId = ActionParameters.Key<Long>("item_id")
        val Outcome = ActionParameters.Key<String>("outcome")
    }
}
