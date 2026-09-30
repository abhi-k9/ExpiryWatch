package io.github.abhik9.expirywatch.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink
import io.github.abhik9.expirywatch.core.navigation.ItemsNavKey
import io.github.abhik9.expirywatch.core.navigation.Navigator
import io.github.abhik9.expirywatch.feature.editor.navigation.editorEntry
import io.github.abhik9.expirywatch.feature.insights.navigation.insightsEntry
import io.github.abhik9.expirywatch.feature.items.navigation.itemsEntry
import io.github.abhik9.expirywatch.feature.settings.navigation.settingsEntries
import io.github.abhik9.expirywatch.navigation.TopLevelDestination

/**
 * The app shell: an adaptive navigation bar (or rail on wide screens) around the screen on top
 * of the back stack. The bar is hidden on detail screens such as the editor.
 */
@Composable
fun ExpiryWatchApp(
    remindersEnabled: Boolean,
    pendingDeepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
) {
    val backStack = rememberNavBackStack(ItemsNavKey())
    val navigator = remember(backStack) { Navigator(backStack) }

    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let {
            navigator.handleDeepLink(it)
            onDeepLinkHandled()
        }
    }
    if (remindersEnabled) NotificationPermissionRequest()

    val currentKey = backStack.lastOrNull() ?: ItemsNavKey()
    val currentTopLevelKey = navigator.currentTopLevelKey
    val layoutType = if (Navigator.isTopLevel(currentKey)) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination.matches(currentTopLevelKey)
                item(
                    selected = selected,
                    onClick = { navigator.navigate(destination.key) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        layoutType = layoutType,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                itemsEntry(navigator)
                editorEntry(navigator)
                insightsEntry()
                settingsEntries(navigator)
            },
        )
    }
}

/**
 * Reminders are on by default, so on Android 13+ ask once per launch for permission to post
 * them. The system stops showing the prompt after the user declines twice.
 */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
