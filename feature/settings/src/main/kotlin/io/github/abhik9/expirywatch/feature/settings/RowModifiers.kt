package io.github.abhik9.expirywatch.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

internal fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(role = Role.Button, onClick = onClick)

/** Makes the whole row toggle the switch, and announces it as a switch. */
internal fun Modifier.toggleableRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit): Modifier =
    toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
