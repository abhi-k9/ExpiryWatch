package io.github.abhik9.expirywatch.feature.settings.advanced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.model.ProductGrouping
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.feature.settings.R
import io.github.abhik9.expirywatch.feature.settings.SwitchRow

@Composable
internal fun AdvancedSettingsRoute(onBack: () -> Unit, viewModel: AdvancedSettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdvancedSettingsScreen(
        settings = settings,
        onProductGroupingChange = viewModel::setProductGrouping,
        onKeepProductsTogetherChange = viewModel::setKeepProductsTogether,
        onRemindAboutExpiredChange = viewModel::setRemindAboutExpired,
        onOnlineProductLookupChange = viewModel::setOnlineProductLookup,
        onBack = onBack,
    )
}

/** Options most people never need to change, so they don't crowd the main settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancedSettingsScreen(
    settings: UserSettings?,
    onProductGroupingChange: (ProductGrouping) -> Unit,
    onKeepProductsTogetherChange: (Boolean) -> Unit,
    onRemindAboutExpiredChange: (Boolean) -> Unit,
    onOnlineProductLookupChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_settings_advanced)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (settings == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.feature_settings_section_item_list))
            ListItem(
                headlineContent = { Text(stringResource(R.string.feature_settings_grouping)) },
                supportingContent = { Text(stringResource(R.string.feature_settings_grouping_summary)) },
                leadingContent = { Icon(Icons.Outlined.Layers, contentDescription = null) },
            )
            Column(modifier = Modifier.selectableGroup()) {
                ProductGrouping.entries.forEach { grouping ->
                    RadioRow(
                        title = stringResource(grouping.title),
                        subtitle = stringResource(grouping.summary),
                        selected = settings.productGrouping == grouping,
                        onClick = { onProductGroupingChange(grouping) },
                    )
                }
            }
            SwitchRow(
                icon = Icons.Outlined.ViewAgenda,
                title = stringResource(R.string.feature_settings_keep_together),
                subtitle = stringResource(R.string.feature_settings_keep_together_summary),
                checked = settings.keepProductsTogether,
                enabled = settings.productGrouping != ProductGrouping.OFF,
                onCheckedChange = onKeepProductsTogetherChange,
            )

            SectionHeader(stringResource(R.string.feature_settings_section_reminders))
            SwitchRow(
                icon = Icons.Outlined.EventBusy,
                title = stringResource(R.string.feature_settings_remind_expired),
                subtitle = stringResource(R.string.feature_settings_remind_expired_summary),
                checked = settings.remindAboutExpired,
                onCheckedChange = onRemindAboutExpiredChange,
            )

            SectionHeader(stringResource(R.string.feature_settings_section_scanning))
            SwitchRow(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.feature_settings_online_lookup),
                subtitle = stringResource(R.string.feature_settings_online_lookup_summary),
                checked = settings.onlineProductLookup,
                onCheckedChange = onOnlineProductLookupChange,
            )
        }
    }
}

@Composable
private fun RadioRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

private val ProductGrouping.title: Int
    get() = when (this) {
        ProductGrouping.OFF -> R.string.feature_settings_grouping_off
        ProductGrouping.NAME -> R.string.feature_settings_grouping_name
        ProductGrouping.NAME_AND_BRAND -> R.string.feature_settings_grouping_name_and_brand
    }

private val ProductGrouping.summary: Int
    get() = when (this) {
        ProductGrouping.OFF -> R.string.feature_settings_grouping_off_summary
        ProductGrouping.NAME -> R.string.feature_settings_grouping_name_summary
        ProductGrouping.NAME_AND_BRAND -> R.string.feature_settings_grouping_name_and_brand_summary
    }
