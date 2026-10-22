package io.github.abhik9.expirywatch.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Remembers whether a text field had focus, so it gets focus and the keyboard back after the
 * activity is recreated, e.g. on rotation. Apply it with [restoreFocus].
 */
@Stable
class FocusRestorer internal constructor(
    internal val requester: FocusRequester,
    internal val focused: MutableState<Boolean>,
)

@Composable
fun rememberFocusRestorer(): FocusRestorer {
    val focused = rememberSaveable { mutableStateOf(false) }
    val restorer = remember { FocusRestorer(FocusRequester(), focused) }
    // What was saved before the activity was recreated, read before the field reports it's unfocused.
    val wasFocused = remember { focused.value }
    LaunchedEffect(restorer) {
        if (wasFocused) restorer.requester.requestFocus()
    }
    return restorer
}

/** Lets [restorer] give this field its focus back; see [FocusRestorer]. */
fun Modifier.restoreFocus(restorer: FocusRestorer): Modifier =
    focusRequester(restorer.requester).onFocusChanged { restorer.focused.value = it.isFocused }
