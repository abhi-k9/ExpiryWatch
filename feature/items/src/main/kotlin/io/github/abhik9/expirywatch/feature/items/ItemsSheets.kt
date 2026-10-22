package io.github.abhik9.expirywatch.feature.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.ui.colors
import io.github.abhik9.expirywatch.core.ui.formatMedium
import java.time.LocalDate

/**
 * How long to leave [items] out of the daily reminder: a week, a month, until the user turns
 * them back on, or until a date they pick. Their reminders can be turned back on when some are
 * snoozed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SnoozeSheet(
    items: List<Item>,
    today: LocalDate,
    onSnooze: (SnoozeLength) -> Unit,
    onPickDate: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = items.singleOrNull()
                    ?.let { stringResource(R.string.feature_items_snooze_title_one, it.name) }
                    ?: pluralStringResource(R.plurals.feature_items_snooze_title_many, items.size, items.size),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.feature_items_snooze_help),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SnoozeOption(
                icon = Icons.Outlined.Snooze,
                text = stringResource(R.string.feature_items_snooze_week),
                supportingText = stringResource(
                    R.string.feature_items_snooze_back_on,
                    SnoozeLength.ONE_WEEK.resumeDate(today).formatMedium(),
                ),
                onClick = { onSnooze(SnoozeLength.ONE_WEEK) },
            )
            SnoozeOption(
                icon = Icons.Outlined.Snooze,
                text = stringResource(R.string.feature_items_snooze_month),
                supportingText = stringResource(
                    R.string.feature_items_snooze_back_on,
                    SnoozeLength.ONE_MONTH.resumeDate(today).formatMedium(),
                ),
                onClick = { onSnooze(SnoozeLength.ONE_MONTH) },
            )
            SnoozeOption(
                icon = Icons.Outlined.NotificationsOff,
                text = stringResource(R.string.feature_items_snooze_indefinitely),
                onClick = { onSnooze(SnoozeLength.INDEFINITELY) },
            )
            SnoozeOption(
                icon = Icons.Outlined.Event,
                text = stringResource(R.string.feature_items_snooze_until_date),
                onClick = onPickDate,
            )
            if (items.any { it.isReminderSnoozed(today) }) {
                SnoozeOption(
                    icon = Icons.Outlined.NotificationsActive,
                    text = stringResource(R.string.feature_items_resume_reminders),
                    onClick = onResume,
                )
            }
        }
    }
}

/**
 * Which of the user's [groups] to move the selected items to: one of them, a new one, or no group
 * when [canUngroup] because some of the items are in one.
 */
@Composable
internal fun MoveToGroupDialog(
    groups: List<CustomGroup>,
    canUngroup: Boolean,
    onPick: (CustomGroup?) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_items_move_to_group)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(groups, key = { it.id }) { group ->
                    ListItem(
                        headlineContent = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.clickable { onPick(group) },
                        leadingContent = { EmojiAvatar(emoji = group.emoji, size = 36.dp) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                item(key = "new") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.feature_items_new_group_option)) },
                        modifier = Modifier.clickable(onClick = onNewGroup),
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                if (canUngroup) {
                    item(key = "none") {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.feature_items_remove_from_group)) },
                            modifier = Modifier.clickable { onPick(null) },
                            leadingContent = { Icon(Icons.Filled.Close, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_items_cancel)) }
        },
    )
}

/** Warns that reminders may not arrive, naming the most serious of the [problems]. */
@Composable
internal fun ReminderProblemsBanner(
    problems: List<ReminderProblem>,
    onFix: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(R.string.feature_items_reminder_problem_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(problems.first().messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_items_reminder_problem_dismiss)) }
            TextButton(onClick = onFix) { Text(stringResource(R.string.feature_items_reminder_problem_fix)) }
        }
    }
}

private fun ReminderProblem.messageRes(): Int = when (this) {
    ReminderProblem.NOTIFICATIONS_BLOCKED -> R.string.feature_items_reminder_problem_notifications
    ReminderProblem.EXACT_ALARMS_BLOCKED -> R.string.feature_items_reminder_problem_alarms
    ReminderProblem.BACKGROUND_RESTRICTED -> R.string.feature_items_reminder_problem_background
    ReminderProblem.LATE -> R.string.feature_items_reminder_problem_late
}

@Composable
private fun SnoozeOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    ListItem(
        headlineContent = { Text(text) },
        modifier = Modifier.clickable(onClick = onClick),
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
